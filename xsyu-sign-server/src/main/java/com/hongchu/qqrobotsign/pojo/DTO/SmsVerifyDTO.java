package com.hongchu.qqrobotsign.pojo.DTO;

import lombok.Data;

@Data
public class SmsVerifyDTO {
    private String smsSessionId;
    private String username;
    private String phone;
    private String smsCode;
}
