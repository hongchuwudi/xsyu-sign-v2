package com.hongchu.qqrobotsign.pojo.VO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CasCaptchaVO {
    private String captchaSessionId;
    private String captchaImageBase64;
    private long expiresIn;
    private String message;
}
