package com.hongchu.qqrobotsign.enums;

/**
 * <p>
 * CAS 登录错误类型枚举
 * </p>
 *
 * @author hongchu
 * @since 2025-11-18
 */
public enum CasErrorType {
    SUCCESS,            // 登录成功
    CAPTCHA_REQUIRED,   // 需要验证码
    ACCOUNT_LOCKED,     // 账号被锁定
    WRONG_PASSWORD,     // 密码错误
    OTHER_ERROR         // 其他错误
}