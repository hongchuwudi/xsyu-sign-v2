package com.hongchu.qqrobotsign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hongchu.qqrobotsign.enums.CasErrorType;
import com.hongchu.qqrobotsign.config.props.CredentialEncryptionProperties;
import com.hongchu.qqrobotsign.exception.BusinessException;
import com.hongchu.qqrobotsign.mapper.UserMapper;
import com.hongchu.qqrobotsign.pojo.DTO.UserDTO;
import com.hongchu.qqrobotsign.pojo.VO.UserVO;
import com.hongchu.qqrobotsign.pojo.entity.SignItem;
import com.hongchu.qqrobotsign.pojo.entity.User;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.service.EmailService;
import com.hongchu.qqrobotsign.service.IAdminService;
import com.hongchu.qqrobotsign.utils.CasLoginResult;
import com.hongchu.qqrobotsign.utils.CryptoUtils;
import com.hongchu.qqrobotsign.utils.XSYULoginUtil;
import com.hongchu.qqrobotsign.webClient.BaseSignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 管理员服务实现类
 * </p>
 *
 * @author hongchu
 * @since 2025-11-23
 */
@Service
@Slf4j
public class AdminServiceImpl extends ServiceImpl<UserMapper, User> implements IAdminService {
    @Autowired EmailService emailService;
    @Autowired BaseSignService baseSignService;
    @Autowired UserServiceImpl userService;
    @Autowired com.hongchu.qqrobotsign.service.IOperationLogService operationLogService;
    @Autowired CredentialEncryptionProperties credentialEncryptionProperties;

    @Override
    public IPage<UserVO> getUsersByPage(Page<User> page, String keyword, String filter) {
        log.info("service层-分页查询用户列表-keyword: {}, filter: {}", keyword, filter);

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        // 筛选条件
        if ("autoSign".equals(filter)) {
            queryWrapper.eq("auto_sign", true);
        } else if ("noJws".equals(filter)) {
            queryWrapper.and(wrapper -> wrapper.isNull("jws").or().eq("jws", ""));
        }

        // 关键词搜索（用户名、姓名、邮箱）
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("username", keyword)
                    .or()
                    .like("name", keyword)
                    .or()
                    .like("email", keyword)
            );
        }

        // 按ID降序排序
        queryWrapper.orderByDesc("id");

        IPage<User> userPage = this.page(page, queryWrapper);

        return userPage.convert(user -> UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .autoSign(user.getAutoSign())
                .signDays(user.getSignDays())
                .signStartTime(user.getSignStartTime() != null ? user.getSignStartTime().toString() : null)
                .signEndTime(user.getSignEndTime() != null ? user.getSignEndTime().toString() : null)
                .jws(StringUtils.isNotBlank(user.getJws()))
                .jwsRefreshedAt(user.getJwsRefreshedAt() != null ? user.getJwsRefreshedAt().toString() : null)
                .updatedAt(user.getUpdatedAt())
                .build());
    }

    @Override
    public void refreshUserJws(String username) {
        log.info("service层-给用户续签JWS-username: {}", username);
        // 复用用户侧续签逻辑（基于 stuPassword）
        userService.refreshJws(username);
    }

    @Override
    public void deleteUser(String username) {
        log.info("service层-删除用户-username: {}", username);
        User user = this.getOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) throw new BusinessException("用户不存在");

        boolean removed = this.remove(new QueryWrapper<User>().eq("username", username));
        if (removed) {
            log.info("用户 {} 删除成功", username);
        } else {
            log.error("用户 {} 删除失败", username);
            throw new BusinessException("用户删除失败");
        }
    }

    @Override
    public void updateUserInfo(String username, UserDTO userDTO) {
        log.info("service层-修改用户信息-username: {}, UserDTO: {}", username, userDTO);
        User user = this.getOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) throw new BusinessException("用户不存在");

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUsername, username);

        User updateUser = new User();

        if (StringUtils.isNotBlank(userDTO.getName()))
            updateUser.setName(userDTO.getName());
        if (StringUtils.isNotBlank(userDTO.getEmail())) {
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
            if (!userDTO.getEmail().matches(emailRegex))
                throw new BusinessException("邮箱格式不正确");
            updateUser.setEmail(userDTO.getEmail());
        }
        if (StringUtils.isNotBlank(userDTO.getSignDays()))
            updateUser.setSignDays(userDTO.getSignDays());
        if (StringUtils.isNotBlank(userDTO.getSignStartTime()))
            updateUser.setSignStartTime(LocalTime.parse(userDTO.getSignStartTime()));
        if (StringUtils.isNotBlank(userDTO.getSignEndTime()))
            updateUser.setSignEndTime(LocalTime.parse(userDTO.getSignEndTime()));

        // 校验签到时间范围
        LocalTime start = updateUser.getSignStartTime() != null
                ? updateUser.getSignStartTime() : user.getSignStartTime();
        LocalTime end = updateUser.getSignEndTime() != null
                ? updateUser.getSignEndTime() : user.getSignEndTime();
        if (start != null && start.isBefore(LocalTime.of(18, 30)))
            throw new BusinessException("签到开始时间不能早于18:30");
        if (end != null && end.isAfter(LocalTime.of(23, 59)))
            throw new BusinessException("签到结束时间不能晚于23:59");
        if (start != null && end != null && !start.isBefore(end))
            throw new BusinessException("签到开始时间必须早于结束时间");

        boolean updated = this.update(updateUser, updateWrapper);
        if (!updated) throw new BusinessException("用户信息更新失败");
    }

    @Override
    public UserVO getUserInfo(String username) {
        log.info("service层-获取用户信息-username: {}", username);
        User user = this.getOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) throw new BusinessException("用户不存在");

        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .autoSign(user.getAutoSign())
                .signDays(user.getSignDays())
                .signStartTime(user.getSignStartTime() != null ? user.getSignStartTime().toString() : null)
                .signEndTime(user.getSignEndTime() != null ? user.getSignEndTime().toString() : null)
                .jws(StringUtils.isNotBlank(user.getJws()))
                .jwsRefreshedAt(user.getJwsRefreshedAt() != null ? user.getJwsRefreshedAt().toString() : null)
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    public void toggleAutoSign(String username, Boolean autoSign) {
        log.info("service层-切换用户自动签到状态-username: {}, autoSign: {}", username, autoSign);
        User user = this.getOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) throw new BusinessException("用户不存在");

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUsername, username)
                .set(User::getAutoSign, autoSign);

        boolean updated = this.update(updateWrapper);
        if (!updated) throw new BusinessException("自动签到状态更新失败");
        log.info("用户 {} 自动签到状态已切换为: {}", username, autoSign);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(UserDTO userDTO) throws InterruptedException {
        log.info("service层-新增用户-username: {}", userDTO.getUsername());

        // 检查用户名是否已存在
        User existUser = this.getOne(new QueryWrapper<User>().eq("username", userDTO.getUsername()));
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 1. 调用 XSYULoginUtil 登录获取 JWS
        CasLoginResult result = XSYULoginUtil.login(userDTO.getUsername(), userDTO.getPassword());
        if (result.getErrorType() != CasErrorType.SUCCESS) throw new BusinessException("登录失败，请检查账号密码");
        String jws = result.getJwsession();

        // 2. 创建新用户：username=学号，保存学校密码（JWS 续签用）
        User newUser = new User();
        newUser.setUsername(userDTO.getUsername());
        newUser.setName(userDTO.getName() != null ? userDTO.getName() : UserServiceImpl.buildDefaultName(userDTO.getUsername()));
        newUser.setStuPassword(CryptoUtils.encrypt(
                userDTO.getPassword(), credentialEncryptionProperties.getMasterKey())
                .getBytes(StandardCharsets.UTF_8));
        newUser.setRole("USER");
        newUser.setJws(jws);
        newUser.setJwsRefreshedAt(LocalDateTime.now());
        newUser.setName(userDTO.getName());
        newUser.setEmail(userDTO.getEmail());
        newUser.setAutoSign(userDTO.getAutoSign() != null ? userDTO.getAutoSign() : false);
        newUser.setSignDays(userDTO.getSignDays() != null ? userDTO.getSignDays() : "0,1,2,3,4,5,6");

        boolean saved = this.save(newUser);
        // 保存后再查签到记录（getAllSign需要从DB读JWS）
        setDefaultSignTimes(newUser, userDTO.getUsername(), jws);
        if (newUser.getSignStartTime() != null) this.updateById(newUser);
        if (!saved) {
            throw new BusinessException("用户添加失败");
        }
        log.info("用户 {} 添加成功---JWS:{}", userDTO.getUsername(), jws);
    }

    /**
     * 从用户最近签到记录推断签到时间范围，无记录则默认 7:00-22:00
     */
    private void setDefaultSignTimes(User user, String username, String jws) {
        try {
            var result = baseSignService.getAllSign(username, 1, 5);
            if (result != null && result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
                for (var item : result.getData()) {
                    if (item.getStart() != null && item.getEnd() != null) {
                        LocalTime startTime = Instant.ofEpochMilli(item.getStart())
                                .atZone(ZoneId.systemDefault()).toLocalTime();
                        LocalTime endTime = Instant.ofEpochMilli(item.getEnd())
                                .atZone(ZoneId.systemDefault()).toLocalTime();
                        user.setSignStartTime(startTime);
                        user.setSignEndTime(endTime);
                        log.info("用户 {} 从签到记录推断时间范围: {} - {}", username, startTime, endTime);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询用户 {} 签到记录失败，使用默认时间范围", username, e);
        }
        user.setSignStartTime(LocalTime.of(19, 0));
        user.setSignEndTime(LocalTime.of(22, 0));
        log.info("用户 {} 使用默认签到时间范围: 07:00 - 22:00", username);
    }

    @Override
    public List<SignItem> getUserSigns(String username, Integer limit) {
        log.info("service层-获取用户签到记录-username: {}, limit: {}", username, limit);
        
        // 检查用户是否存在
        User user = this.getOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) throw new BusinessException("用户不存在");
        
        // 获取用户签到记录
        try {
            Result<List<SignItem>> result = baseSignService.getAllSign(username, 1, limit);
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                return result.getData();
            }
            // 如果JWS失效，尝试刷新
            if (result != null && "未登录,请重新登录".equals(result.getMessage())) {
                userService.refreshJws(username);
                result = baseSignService.getAllSign(username, 1, limit);
                if (result != null && result.getCode() == 200 && result.getData() != null) {
                    return result.getData();
                }
            }
            log.warn("获取用户 {} 签到记录失败: {}", username, result != null ? result.getMessage() : "null result");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("获取用户 {} 签到记录异常", username, e);
            return Collections.emptyList();
        }
    }

    @Override
    public java.util.Map<String, Object> getUserStats() {
        List<User> allUsers = this.list();
        long todayStart = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli();

        long autoSignCount = allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getAutoSign())).count();
        long invalidJwsCount = allUsers.stream().filter(u -> u.getJws() == null || u.getJws().isEmpty()).count();
        long todayActiveCount = allUsers.stream()
                .filter(u -> u.getUpdatedAt() != null
                        && u.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() >= todayStart)
                .count();

        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("total", allUsers.size());
        stats.put("autoSignCount", autoSignCount);
        stats.put("invalidJwsCount", invalidJwsCount);
        stats.put("todayActiveCount", todayActiveCount);
        return stats;
    }
}
