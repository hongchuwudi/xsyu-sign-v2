package com.hongchu.qqrobotsign.result.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignResultResponse {
    private Integer code;
    private String message;
    private String data;  // 签到成功/失败信息
}