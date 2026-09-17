package com.hongchu.qqrobotsign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hongchu.qqrobotsign.context.BaseContext;
import com.hongchu.qqrobotsign.enums.CasErrorType;
import com.hongchu.qqrobotsign.exception.BusinessException;
import com.hongchu.qqrobotsign.exception.CaptchaRequiredException;
import com.hongchu.qqrobotsign.mapper.UserMapper;
import com.hongchu.qqrobotsign.pojo.DTO.UserDTO;
import com.hongchu.qqrobotsign.pojo.VO.QrPollVO;
import com.hongchu.qqrobotsign.pojo.VO.QrSessionVO;
import com.hongchu.qqrobotsign.pojo.VO.UserLoginVO;
import com.hongchu.qqrobotsign.pojo.VO.UserVO;
import com.hongchu.qqrobotsign.pojo.entity.CasCaptchaState;
import com.hongchu.qqrobotsign.pojo.entity.CasQrState;
import com.hongchu.qqrobotsign.pojo.entity.CasSmsState;
import com.hongchu.qqrobotsign.pojo.entity.User;
import com.hongchu.qqrobotsign.properties.JwtProperties;
import com.hongchu.qqrobotsign.config.props.AdminConfig;
import com.hongchu.qqrobotsign.service.CaptchaService;
import com.hongchu.qqrobotsign.service.CasQrSessionService;
import com.hongchu.qqrobotsign.service.CasSmsSessionService;
import com.hongchu.qqrobotsign.service.EmailService;
import com.hongchu.qqrobotsign.service.IUserService;
import com.hongchu.qqrobotsign.utils.CasLoginResult;
import com.hongchu.qqrobotsign.utils.CryptoUtils;
import com.hongchu.qqrobotsign.utils.JwtUtil;
import com.hongchu.qqrobotsign.utils.RSAUtils;
import com.hongchu.qqrobotsign.utils.XSYULoginUtil;
import com.hongchu.qqrobotsign.webClient.GwxgUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    @Autowired CaptchaService captchaService;
    @Autowired CasSmsSessionService casSmsSessionService;
    @Autowired CasQrSessionService casQrSessionService;

    private static final String HASH_PREFIX = "310000:";

    private String decryptPsd(String psd) {
        try {
            return RSAUtils.decrypt(psd, rsaConfig.getPrivateKey());
        } catch (Exception e) {
            log.error("RSA解密密码失败: {}", e.getMessage(), e);
            throw new BusinessException("密码解密失败，请重试");
        }
    }

    /** 管理员登录：本地密码校验（不经过 CAS），兼容旧 AES 格式并迁移为 PBKDF2 */
    private UserLoginVO adminLogin(String username, String password) {
        User user = this.getOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) throw new BusinessException("管理员账号不存在，请联系系统管理员");
        String stored = user.getPassword() != null ? new String(user.getPassword(), StandardCharsets.UTF_8) : null;
        if (stored == null) throw new BusinessException("管理员账号未设置密码");

        if (stored.startsWith(HASH_PREFIX)) {
            if (!CryptoUtils.verifyPasswordHash(password, stored)) throw new BusinessException("密码错误");
        } else {
            String legacy;
            try {
                legacy = CryptoUtils.decrypt(stored);
            } catch (Exception e) {
                throw new BusinessException("密码错误");
            }
            if (!legacy.equals(password)) throw new BusinessException("密码错误");
            user.setPassword(CryptoUtils.generatePasswordHash(password).getBytes(StandardCharsets.UTF_8));
        }
        if (!"ADMIN".equals(user.getRole())) user.setRole("ADMIN");
        this.updateById(user);
        log.info("管理员 {} 登录成功", username);
        return buildLoginVO(user);
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
        if (user.getStuPassword() == null) {
            long duration = System.currentTimeMillis() - start;
            log.warn("用户: {} 无学校密码(短信/扫码登录)，无法自动续签JWS", username);
            if (StringUtils.isNotBlank(user.getEmail())) {
                emailService.sendErrorJwsRefreshMes(user.getEmail(), username);
            }
            operationLogService.save("JWS_REFRESH", "JWS续签",
                    "用户: " + username + " 无学校密码，无法自动续签", "FAIL", "SYSTEM", null, duration);
            throw new BusinessException("无学校密码，请用密码方式登录一次以保存");
        }
        String pass = CryptoUtils.decrypt(new String(user.getStuPassword()));
        CasLoginResult result = XSYULoginUtil.login(user.getUsername(), pass);
        if (result.getErrorType() != CasErrorType.SUCCESS) {
            long duration = System.currentTimeMillis() - start;
            log.error("用户: {} CAS登录失败，无法续签JWS - {}", username, result.getErrorType());
            if (StringUtils.isNotBlank(user.getEmail())) {
                emailService.sendErrorJwsRefreshMes(user.getEmail(), username);
            }
            operationLogService.save("JWS_REFRESH", "JWS续签",
                    "用户: " + username + " CAS登录失败(" + result.getErrorType() + ")", "FAIL", "SYSTEM", null, duration);
            throw new BusinessException("JWS续签失败：CAS登录失败，请检查学号密码是否变更");
        }
        String jws = result.getJwsession();
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

    // ==================== 学号绑定 ====================

    private User currentUser() {
        User user = this.getById(BaseContext.getCurrentId());
        if (user == null) throw new BusinessException("用户不存在");
        return user;
    }

    /** 绑定成功后保存并返回登录信息 */
    private UserLoginVO saveBinding(User user, String casPassword, String jws, String name, String phone) {
        if (casPassword != null) {
            user.setStuPassword(CryptoUtils.encrypt(casPassword).getBytes(StandardCharsets.UTF_8));
        }
        if (phone != null) user.setPhone(phone);
        if (name != null && !name.isBlank() && (user.getName() == null || user.getName().equals(user.getUsername()))) {
            user.setName(name);
        }
        user.setJws(jws);
        user.setJwsRefreshedAt(LocalDateTime.now());
        this.updateById(user);
        setDefaultSignTimes(user, user.getUsername());
        this.updateById(user);
        log.info("用户 {} 更新学校密码成功", user.getUsername());
        return buildLoginVO(user);
    }

    /** 校验 JWSESSION 归属的学号与输入一致 */
    private void verifySessionOwner(String jws, String username) {
        GwxgUserInfo info = baseSignService.getCurrentUserInfo(jws);
        if (info == null || info.getUsername() == null) {
            log.warn("无法获取CAS用户信息校验学号归属, jws前8位: {}", jws.substring(0, Math.min(8, jws.length())));
            return;
        }
        if (!info.getUsername().equals(username)) {
            throw new BusinessException("CAS会话与学号不匹配(会话属于 " + info.getUsername() + ")，请确认学号");
        }
    }

    /** CAS 密码登录核心：登录失败抛业务异常；触发验证码抛 CaptchaRequiredException(1004) */
    private CasLoginResult doCasPasswordLogin(String username, String casPassword, String captchaSessionId, String captchaCode) {
        CasLoginResult result;
        if (StringUtils.isNotBlank(captchaSessionId)) {
            CasCaptchaState state = captchaService.getState(captchaSessionId);
            if (state == null) throw new BusinessException("验证码已过期，请重新提交");
            if (!username.equals(state.getUsername())) throw new BusinessException("会话信息不匹配，请重新提交");
            try {
                result = XSYULoginUtil.loginWithCaptcha(
                        username, casPassword, captchaCode, state.getExecution(),
                        XSYULoginUtil.toCookieMap(state.getCookies()));
            } finally {
                captchaService.deleteState(captchaSessionId);
            }
        } else {
            result = XSYULoginUtil.login(username, casPassword);
        }

        if (result.getErrorType() == CasErrorType.CAPTCHA_REQUIRED) {
            CasCaptchaState state = result.getCaptchaState();
            String sessionId = captchaService.saveState(state);
            throw new CaptchaRequiredException(sessionId, result.getCaptchaImageBase64());
        }
        if (result.getErrorType() == CasErrorType.ACCOUNT_LOCKED) {
            throw new BusinessException("学号已被学校系统锁定，请稍后再试或改用短信/扫码");
        }
        if (result.getErrorType() != CasErrorType.SUCCESS) {
            throw new BusinessException("CAS验证失败：" + (result.getErrorMessage() != null ? result.getErrorMessage() : "请检查学号和密码"));
        }
        return result;
    }

    /** 更新学校密码（用 CAS 密码验证当前账号，保存供自动续签；带验证码支持） */
    @Override
    public UserLoginVO bindByPassword(String casPsd, String captchaSessionId, String captchaCode) {
        User user = currentUser();
        String casPassword = decryptPsd(casPsd);
        CasLoginResult result = doCasPasswordLogin(user.getUsername(), casPassword, captchaSessionId, captchaCode);
        return saveBinding(user, casPassword, result.getJwsession(), null, null);
    }

    /** 学号+学校密码登录（登录页入口；管理员走本地密码校验；触发验证码抛 1004；无账号自动创建） */
    @Override
    public UserLoginVO xsyPasswordLogin(String username, String casPsd, String captchaSessionId, String captchaCode) {
        String casPassword = decryptPsd(casPsd);
        if (adminConfig.getUsername().equalsIgnoreCase(username)) {
            return adminLogin(username, casPassword);
        }
        CasLoginResult result = doCasPasswordLogin(username, casPassword, captchaSessionId, captchaCode);
        verifySessionOwner(result.getJwsession(), username);
        return finishCasLogin(result.getJwsession(), username, null, casPassword);
    }

    /** 短信登录：发送验证码 */
    @Override
    public String smsSendCode(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("手机号格式不正确");
        }
        Map<String, String> cookies = new LinkedHashMap<>();
        try {
            XSYULoginUtil.openCasPage(cookies);
            String err = XSYULoginUtil.sendSmsCode(phone, cookies);
            if (err != null) throw new BusinessException(err);

            CasSmsState state = new CasSmsState();
            state.setPhone(phone);
            state.setCookies(XSYULoginUtil.toCookieEntries(cookies));
            state.setCreatedAt(System.currentTimeMillis());
            return casSmsSessionService.saveState(state);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("短信发送异常 - phone: {} - {}", phone, e.getMessage(), e);
            throw new BusinessException("短信发送失败，请稍后重试");
        }
    }

    /** 短信登录：校验验证码并按学号匹配账号（无账号自动创建） */
    @Override
    public UserLoginVO smsLogin(String smsSessionId, String username, String phone, String smsCode) {
        if (StringUtils.isBlank(username)) throw new BusinessException("请先输入学号");
        CasSmsState state = casSmsSessionService.getState(smsSessionId);
        if (state == null) throw new BusinessException("短信会话已过期，请重新获取验证码");
        if (!state.getPhone().equals(phone)) throw new BusinessException("手机号与会话不匹配");

        Map<String, String> cookies = XSYULoginUtil.toCookieMap(state.getCookies());
        try {
            XSYULoginUtil.SmsValidResult valid = XSYULoginUtil.smsValid(phone, smsCode, cookies);
            log.info("短信登录校验结果 - username: {}, phone: {}, code: {}, multiple: {}, msg: {}",
                    username, phone, valid.getCode(), valid.isMultiple(), valid.getMsg());
            if (valid.getCode() != 1) {
                String msg = valid.getMsg() != null ? valid.getMsg() : "短信验证码错误或已过期";
                // 学校的"认证信息无效"对用户没有指导意义，转成明确提示
                if (msg.contains("认证信息无效")) msg = "短信验证码错误或已过期，请核对后重试";
                throw new BusinessException(msg);
            }
            if (valid.isMultiple()) {
                if (valid.getAccounts().isEmpty()) {
                    // 手机号未绑定任何账号：走手动输入账号路径
                    String err = XSYULoginUtil.accountValid(username, false, cookies);
                    if (err != null) throw new BusinessException(err);
                } else {
                    boolean containsStu = valid.getAccounts().stream().anyMatch(a -> username.equals(a.getCode()));
                    if (!containsStu) {
                        throw new BusinessException("该手机号未绑定学号 " + username + "，请检查输入");
                    }
                    String err = XSYULoginUtil.accountValid(username, false, cookies);
                    if (err != null) throw new BusinessException(err);
                }
            }
            return finishSmsLogin(smsSessionId, username, phone, cookies);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("短信登录校验异常 - {}", e.getMessage(), e);
            throw new BusinessException("短信登录失败，请重试");
        } finally {
            casSmsSessionService.refreshTtl(smsSessionId);
        }
    }

    private UserLoginVO finishSmsLogin(String smsSessionId, String expectUsername, String phone, Map<String, String> cookies) {
        try {
            String ticket = XSYULoginUtil.getTicketAfterAuth(cookies);
            if (ticket == null) {
                for (int i = 0; i < 3 && ticket == null; i++) {
                    Thread.sleep(500);
                    ticket = XSYULoginUtil.getTicketAfterAuth(cookies);
                }
            }
            if (ticket == null) throw new BusinessException("认证未完成，请重试");

            String jws = XSYULoginUtil.completeTicketFlow(ticket, cookies);
            if (jws == null) throw new BusinessException("获取JWSESSION失败");
            return finishCasLogin(jws, expectUsername, phone, null);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("短信登录完成阶段异常 - {}", e.getMessage(), e);
            throw new BusinessException("登录失败，请重试");
        } finally {
            casSmsSessionService.deleteState(smsSessionId);
        }
    }

    /** 扫码登录：创建二维码会话（需先输入学号） */
    @Override
    public QrSessionVO qrCreate(String username) {
        if (StringUtils.isBlank(username)) throw new BusinessException("请先输入学号");
        Map<String, String> cookies = new LinkedHashMap<>();
        try {
            XSYULoginUtil.CasPage page = XSYULoginUtil.openCasPage(cookies);
            if (page.getLoginLt() == null) throw new BusinessException("CAS未返回二维码参数");
            byte[] qrBytes = XSYULoginUtil.generateQrImage(page.getLoginLt(), cookies);

            CasQrState state = new CasQrState();
            state.setLoginLt(page.getLoginLt());
            state.setUsername(username);
            state.setCookies(XSYULoginUtil.toCookieEntries(cookies));
            state.setCreatedAt(System.currentTimeMillis());
            String sessionId = casQrSessionService.saveState(state);

            return QrSessionVO.builder()
                    .qrSessionId(sessionId)
                    .qrImageBase64("data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes))
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建扫码会话异常 - {}", e.getMessage(), e);
            throw new BusinessException("创建扫码会话失败，请重试");
        }
    }

    /** 扫码登录：轮询扫码状态 */
    @Override
    public QrPollVO qrPoll(String qrSessionId) {
        CasQrState state = casQrSessionService.getState(qrSessionId);
        if (state == null) return QrPollVO.builder().status("EXPIRED").build();

        casQrSessionService.refreshTtl(qrSessionId);
        Map<String, String> cookies = XSYULoginUtil.toCookieMap(state.getCookies());
        try {
            String poll = XSYULoginUtil.pollAnalogLogin(cookies);
            if (!"success".equalsIgnoreCase(poll)) {
                return QrPollVO.builder().status("WAITING").build();
            }

            String ticket = null;
            for (int i = 0; i < 3 && ticket == null; i++) {
                if (i > 0) Thread.sleep(500);
                ticket = XSYULoginUtil.getTicketAfterAuth(cookies);
            }
            if (ticket == null) throw new BusinessException("扫码确认后获取票据失败，请重试");

            String jws = XSYULoginUtil.completeTicketFlow(ticket, cookies);
            if (jws == null) throw new BusinessException("获取JWSESSION失败");
            casQrSessionService.deleteState(qrSessionId);
            UserLoginVO loginVO = finishCasLogin(jws, state.getUsername(), null, null);
            return QrPollVO.builder().status("SUCCESS").loginVO(loginVO).build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("扫码轮询异常 - {}", e.getMessage(), e);
            throw new BusinessException("扫码状态查询失败，请重试");
        }
    }

    /** CAS 认证完成后的收尾：按学号查找或创建账号，更新 JWS，返回登录信息 */
    private UserLoginVO finishCasLogin(String jws, String expectUsername, String phone, String casPassword) {
        GwxgUserInfo info = baseSignService.getCurrentUserInfo(jws);
        if (info == null || info.getUsername() == null) {
            throw new BusinessException("无法获取学号信息，请重试");
        }
        if (expectUsername != null && !expectUsername.equals(info.getUsername())) {
            throw new BusinessException("CAS会话与输入的学号不匹配");
        }

        User user = findOrCreateUser(info.getUsername(), info.getName(), phone);
        if (info.getName() != null && !info.getName().isBlank()
                && (user.getName() == null || user.getName().equals(user.getUsername()))) {
            user.setName(info.getName());
        }
        if (phone != null) user.setPhone(phone);
        if (casPassword != null) {
            user.setStuPassword(CryptoUtils.encrypt(casPassword).getBytes(StandardCharsets.UTF_8));
        }
        user.setJws(jws);
        user.setJwsRefreshedAt(LocalDateTime.now());
        this.updateById(user);
        if (user.getSignStartTime() == null) {
            setDefaultSignTimes(user, info.getUsername());
            this.updateById(user);
        }
        log.info("用户 {} CAS登录成功", user.getUsername());
        return buildLoginVO(user);
    }

    /** 按学号（username）找账号；找不到则自动创建（登录即注册） */
    private User findOrCreateUser(String username, String name, String phone) {
        User user = this.getOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            user = new User();
            user.setUsername(username);
            user.setName(name != null && !name.isBlank() ? name : buildDefaultName(username));
            if (phone != null) user.setPhone(phone);
            user.setRole("USER");
            this.save(user);
            log.info("登录自动创建账号: {}", username);
        }
        return user;
    }

    private UserLoginVO buildLoginVO(User user) {
        String secretKey = jwtProperties.getSecretKey();
        long ttl = jwtProperties.getTtl();
        Map<String, Object> map = new HashMap<>();
        map.put("username", user.getUsername());
        map.put("userId", user.getId());
        String role = user.getRole() != null ? user.getRole()
                : (adminConfig.getUsername().equalsIgnoreCase(user.getUsername()) ? "ADMIN" : "USER");
        map.put("role", role);
        String jwt = JwtUtil.createJWT(secretKey, ttl, map);
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
                .phone(user.getPhone())
                .build();
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
                .phone(byId.getPhone())
                .build();
    }
}
