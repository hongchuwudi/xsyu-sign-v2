package com.hongchu.qqrobotsign.exception;

import lombok.Getter;

@Getter
public class CaptchaRequiredException extends RuntimeException {
    private final String captchaSessionId;
    private final String captchaImageBase64;
    private final long expiresIn;

    public CaptchaRequiredException(String captchaSessionId, String captchaImageBase64) {
        super("需要输入验证码");
        this.captchaSessionId = captchaSessionId;
        this.captchaImageBase64 = captchaImageBase64;
        this.expiresIn = 300L;
    }
}
