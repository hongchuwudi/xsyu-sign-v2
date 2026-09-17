package com.hongchu.qqrobotsign.utils;

import com.hongchu.qqrobotsign.enums.CasErrorType;
import com.hongchu.qqrobotsign.pojo.entity.CasCaptchaState;

/**
 * CAS 登录结果封装类。
 * <p>
 * 用于统一封装 CAS 登录过程中的各种返回状态，包括：
 * <ul>
 *     <li>登录成功，返回 jwsession</li>
 *     <li>需要验证码，返回验证码状态及验证码图片 Base64</li>
 *     <li>登录失败，返回错误类型及错误信息</li>
 * </ul>
 * 该类通过静态工厂方法创建实例，外部只能读取结果，不能修改。
 *
 * @author hongchu
 */
public class CasLoginResult {
    /** 错误类型，标识本次登录的结果状态（成功、需要验证码、各种错误等） */
    private CasErrorType errorType;
    /** 登录成功后返回的 jwsession，用于后续请求的身份凭证 */
    private String jwsession;
    /** 错误信息，登录失败时用于描述具体的错误原因 */
    private String errorMessage;
    /** 验证码状态，当需要验证码时携带，用于后续提交验证码时校验 */
    private CasCaptchaState captchaState;
    /** 验证码图片的 Base64 编码，当需要验证码时携带，供前端展示 */
    private String captchaImageBase64;

    /**
     * 构造登录成功的结果。
     *
     * @param jwsession 登录成功后获取的 jwsession
     * @return 登录成功的结果对象
     */
    public static CasLoginResult success(String jwsession) {
        CasLoginResult r = new CasLoginResult();
        r.errorType = CasErrorType.SUCCESS;
        r.jwsession = jwsession;
        return r;
    }

    /**
     * 构造需要验证码的结果。
     *
     * @param state              验证码状态，用于后续校验验证码
     * @param captchaImageBase64 验证码图片的 Base64 编码
     * @return 需要验证码的结果对象
     */
    public static CasLoginResult captchaRequired(CasCaptchaState state, String captchaImageBase64) {
        CasLoginResult r = new CasLoginResult();
        r.errorType = CasErrorType.CAPTCHA_REQUIRED;
        r.captchaState = state;
        r.captchaImageBase64 = captchaImageBase64;
        return r;
    }

    /**
     * 构造登录失败的结果。
     *
     * @param type    错误类型
     * @param message 错误信息
     * @return 登录失败的结果对象
     */
    public static CasLoginResult error(CasErrorType type, String message) {
        CasLoginResult r = new CasLoginResult();
        r.errorType = type;
        r.errorMessage = message;
        return r;
    }
    /** @return 错误类型 */
    public CasErrorType getErrorType() { return errorType; }
    /** @return 登录成功后的 jwsession */
    public String getJwsession() { return jwsession; }
    /** @return 错误信息 */
    public String getErrorMessage() { return errorMessage; }
    /** @return 验证码状态 */
    public CasCaptchaState getCaptchaState() { return captchaState; }
    /** @return 验证码图片的 Base64 编码 */
    public String getCaptchaImageBase64() { return captchaImageBase64; }
}