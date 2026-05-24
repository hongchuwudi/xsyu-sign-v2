package com.hongchu.qqrobotsign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hongchu.qqrobotsign.context.BaseContext;
import com.hongchu.qqrobotsign.exception.BusinessException;
import com.hongchu.qqrobotsign.mapper.UserMapper;
import com.hongchu.qqrobotsign.pojo.DTO.UserDTO;
import com.hongchu.qqrobotsign.pojo.VO.UserLoginVO;
import com.hongchu.qqrobotsign.pojo.VO.UserVO;
import com.hongchu.qqrobotsign.pojo.entity.User;
import com.hongchu.qqrobotsign.properties.JwtProperties;
import com.hongchu.qqrobotsign.config.props.AdminConfig;
import com.hongchu.qqrobotsign.service.EmailService;
import com.hongchu.qqrobotsign.service.IUserService;
import com.hongchu.qqrobotsign.utils.CryptoUtils;
import com.hongchu.qqrobotsign.utils.JwtUtil;
import com.hongchu.qqrobotsign.utils.RSAUtils;
import com.hongchu.qqrobotsign.utils.XSYULoginUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author hongchu
 * @since 2025-11-17
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired JwtProperties jwtProperties;
    @Autowired EmailService emailService;
    @Autowired AdminConfig adminConfig;
    @Autowired com.hongchu.qqrobotsign.config.props.RSAConfig rsaConfig;
    @Lazy @Autowired com.hongchu.qqrobotsign.webClient.BaseSignService baseSignService;
    @Autowired com.hongchu.qqrobotsign.service.IOperationLogService operationLogService;

    // 登录
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginVO register(String username, String psd) throws InterruptedException {
        // 使用RSA私钥解密密码
        String password;
        try {
            log.info("开始RSA解密，密文长度: {}", psd != null ? psd.length() : 0);
            log.debug("密文内容: {}", psd);
            String privateKey = rsaConfig.getPrivateKey();
            log.info("私钥长度: {}", privateKey != null ? privateKey.length() : 0);
            password = RSAUtils.decrypt(psd, privateKey);
            log.info("RSA解密成功，密码长度: {}", password.length());
        } catch (Exception e) {
            log.error("RSA解密密码失败: {}", e.getMessage(), e);
            throw new BusinessException("密码解密失败，请重试: " + e.getMessage());
        }
        log.info("用户: {},密码长度: {}", username, password.length());

        // 判断是否为管理员（不区分大小写）
        boolean isAdmin = adminConfig.getUsername().equalsIgnoreCase(username);

        // 1.检查是否有该用户
        User user = this.getOne(new QueryWrapper<User>().eq("username", username));

        // 2.如果有就更新jws并返回用户
        if (user != null) {
            if (isAdmin) {
                // 管理员：直接登录，不需要调用 XSYULoginUtil
                log.info("管理员: {} 登录，不需要更新JWS", username);
                // 校验数据库密码
                if (user.getPassword() != null) {
//                    log.info("测试-解密后的密码:{}", password);
                    String encryptedPassword = new String(user.getPassword());
                    try {
                        String decryptedPassword = CryptoUtils.decrypt(encryptedPassword);
                        if (!password.equals(decryptedPassword)) {
                            log.warn("管理员: {} 密码错误", username);
                            throw new BusinessException("用户名或密码错误");
                        }
                        log.info("管理员: {} 密码校验成功", username);
                    } catch (Exception e) {
                        log.error("管理员: {} 密码解密失败", username, e);
                        throw new BusinessException("密码验证失败，请联系系统管理员");
                    }
                } else {
                    log.error("管理员: {} 数据库中无密码记录", username);
                    throw new BusinessException("管理员账号未设置密码，请联系系统管理员");
                }
            } else {
                // 普通用户：调用 XSYULoginUtil 验证并获取新的jws
                String jws = XSYULoginUtil.login(username, password);
                if (jws == null) throw new BusinessException("登录失败,请重新尝试");

                // 更新jws
                user.setJws(jws);
                user.setJwsRefreshedAt(LocalDateTime.now());
                log.info("用户: {} 更新JWS成功---JWS:{}", username, jws);

                // 更新数据库
                this.updateById(user);
            }
        } else {
            if (isAdmin) {
                // 管理员不存在，提示联系系统管理员创建账号
                log.warn("管理员: {} 不存在，拒绝注册", username);
                throw new BusinessException("管理员账号不存在，请联系系统管理员创建账号");
            }

            // 普通用户：调用 XSYULoginUtil 验证并获取新的jws
            String jws = XSYULoginUtil.login(username, password);
            if (jws == null) throw new BusinessException("登录失败,请重新尝试");

            // 创建用户
            user = new User();
            user.setUsername(username);
            user.setName(buildDefaultName(username));
            byte[] passwordBytes = CryptoUtils.encrypt(password).getBytes();
            user.setPassword(passwordBytes);
            user.setJws(jws);
            user.setJwsRefreshedAt(LocalDateTime.now());
            this.save(user);
            // 保存后再查签到记录（getAllSign需要从DB读JWS）
            setDefaultSignTimes(user, username);
            this.updateById(user);
            log.info("用户: {} 注册成功---JWS:{}", username, jws);
        }

        // 4. 生成jwt（新用户和已有用户都需要）
        String secretKey = jwtProperties.getSecretKey();
        long ttl = jwtProperties.getTtl();
        Map<String, Object> map = new HashMap<>();
        map.put("username", user.getUsername());
        map.put("userId", user.getId());

        String jwt = JwtUtil.createJWT(secretKey, ttl, map);

        // 判断是否为管理员（不区分大小写）
        String role = adminConfig.getUsername().equalsIgnoreCase(username) ? "ADMIN" : "USER";

        // 封装信息返回
        return UserLoginVO.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .jwt(jwt)
                .autoSign(user.getAutoSign())
                .signDays(user.getSignDays())
                .signStartTime(user.getSignStartTime() != null ? user.getSignStartTime().toString() : null)
                .signEndTime(user.getSignEndTime() != null ? user.getSignEndTime().toString() : null)
                .role(role)
                .build();
    }

    @Override
    public String testLogin(String username, String password,String code) {
        if(!Objects.equals(code, "hongchuiloveu"))
            throw new BusinessException("验证码错误");
        log.info("测试CAS登录 - 用户: {}", username);

        // 直接调用XSYULoginUtil，不经过RSA解密
        String jws = XSYULoginUtil.login(username, password);

        if (jws == null) {
            log.error("测试CAS登录失败 - 用户: {}", username);
            throw new BusinessException("CAS登录失败，请检查用户名密码或验证码");
        }

        log.info("测试CAS登录成功 - 用户: {}, JWS: {}", username, jws);
        return jws;
    }

    // 删除登录信息
    @Override
    public String removeLoginInfo() {
        log.info("删除用户登录信息-userId: {}", BaseContext.getCurrentId());
        long userId = BaseContext.getCurrentId();
        // 1. 检查用户是否存在
        User user = this.getById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        // 2. 删除用户记录
        boolean removed = this.remove(new QueryWrapper<User>().eq("username", user.getUsername()));

        if (removed) {
            log.info("用户 {} 信息删除成功", user.getUsername());
            return "用户信息删除成功";
        } else {
            log.error("用户 {} 信息删除失败", user.getUsername());
            throw new BusinessException("用户信息删除失败");
        }
    }

    // 退出登录（不删除数据库）
    @Override
    public void logout() {
        log.info("用户退出登录-userId: {}", BaseContext.getCurrentId());
        // 退出登录不需要做任何数据库操作
        // 只需要前端清除本地存储的 token 和用户信息即可
        log.info("用户退出登录成功");
    }

    // 刷新JWS
    @Override
    public void refreshJws(String username) {
        long start = System.currentTimeMillis();
        User user = this.getOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            operationLogService.save("JWS_REFRESH", "JWS续签",
                    "用户不存在: " + username, "FAIL", "SYSTEM", null, System.currentTimeMillis() - start);
            throw new BusinessException("用户不存在,无法续签JWS");
        }
        String pass = CryptoUtils.decrypt(new String(user.getPassword()));
        String jws = XSYULoginUtil.login(username, pass);
        if (jws == null) {
            long duration = System.currentTimeMillis() - start;
            log.error("用户: {} CAS登录失败，无法续签JWS", username);
            emailService.sendErrorJwsRefreshMes(user.getEmail(), username);
            operationLogService.save("JWS_REFRESH", "JWS续签",
                    "用户: " + username + " CAS登录失败", "FAIL", "SYSTEM", null, duration);
            throw new BusinessException("JWS续签失败：CAS登录失败，请检查账号密码是否变更");
        }
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUsername, username)
                .set(User::getJws, jws)
                .set(User::getJwsRefreshedAt, LocalDateTime.now());
        boolean updated = this.update(updateWrapper);
        long duration = System.currentTimeMillis() - start;
        if (updated) {
            log.info("用户: {} 续签成功---JWS:{}", username, jws);
            operationLogService.save("JWS_REFRESH", "JWS续签",
                    "用户: " + username + " 续签成功", "SUCCESS", "SYSTEM", null, duration);
        } else {
            log.error("用户: {} 续签失败，数据库更新失败", username);
            emailService.sendErrorJwsRefreshMes(user.getEmail(), username);
            operationLogService.save("JWS_REFRESH", "JWS续签",
                    "用户: " + username + " 数据库更新失败", "FAIL", "SYSTEM", null, duration);
            throw new BusinessException("JWS更新失败");
        }
    }

    // 修改信息
    @Override
    public void setInfo(UserDTO userDTO) {
        long userId = BaseContext.getCurrentId();
        // 参数校验
        User user = this.getById(userId);
        if (user == null) throw new BusinessException("没有改用户");

        // 构建更新条件
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUsername, user.getUsername());

        // 构建更新实体
        User updateUser = new User();

        // 只更新非空字段
        if (StringUtils.isNotBlank(userDTO.getName()))
            updateUser.setName(userDTO.getName());
        if (StringUtils.isNotBlank(userDTO.getEmail())) {
            // 邮箱格式校验
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

        // 执行更新
        boolean updated = update(updateUser, updateWrapper);
        if (!updated) throw new BusinessException("用户信息更新失败，用户不存在或数据未变化");
    }

    /**
     * 根据12位学号生成默认姓名，格式：后10位按 xx-xxxx-xxxx 分割
     * 例：202307070211 → 23-0707-0211
     */
    static String buildDefaultName(String username) {
        if (username == null || username.length() < 12) return username;
        String last10 = username.substring(username.length() - 10);
        return last10.substring(0, 2) + "-" + last10.substring(2, 6) + "-" + last10.substring(6);
    }

    /**
     * 从用户最近签到记录推断签到时间范围，无记录则默认 7:00-22:00
     */
    private void setDefaultSignTimes(User user, String username) {
        try {
            var result = baseSignService.getAllSign(username, 1, 5);
            if (result != null && result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
                var signItems = result.getData();
                // 取第一条（最近的）有 start/end 的记录
                for (var item : signItems) {
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
        // 默认 7:00-22:00
        user.setSignStartTime(LocalTime.of(19, 0));
        user.setSignEndTime(LocalTime.of(22, 0));
        log.info("用户 {} 使用默认签到时间范围: 07:00 - 22:00", username);
    }

    // 设置自动签到
    @Override
    public void setAutoSign(Boolean isAuto) {
        // 拿到用户id
        Long currentId = BaseContext.getCurrentId();
        // 检查用户是否存在
        User user = getById(currentId);
        if (user == null) throw new BusinessException("用户不存在");

        // 执行更新
        lambdaUpdate().eq(User::getUsername, user.getUsername())
                .set(User::getAutoSign, isAuto).update();
    }

    @Override
    public UserVO getMyInfo() {
        Long currentId = BaseContext.getCurrentId();
        User byId = getById(currentId);
        if(byId == null) throw new BusinessException("用户不存在");
        return UserVO.builder()
                .id(byId.getId())
                .username(byId.getUsername())
                .name(byId.getName())
                .email(byId.getEmail())
                .autoSign(byId.getAutoSign())
                .signDays(byId.getSignDays())
                .signStartTime(byId.getSignStartTime() != null ? byId.getSignStartTime().toString() : null)
                .signEndTime(byId.getSignEndTime() != null ? byId.getSignEndTime().toString() : null)
                .build();
    }
}
