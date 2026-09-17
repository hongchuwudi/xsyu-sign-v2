package com.hongchu.qqrobotsign.pojo.DTO;

import lombok.Data;

@Data
public class BindPasswordDTO {
    private String username;
    private String casPsd;
    private String captchaSessionId;
    private String captchaCode;
}
