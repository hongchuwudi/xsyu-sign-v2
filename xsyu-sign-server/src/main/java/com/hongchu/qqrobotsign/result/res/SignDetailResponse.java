package com.hongchu.qqrobotsign.result.res;

import com.hongchu.qqrobotsign.pojo.entity.SignItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignDetailResponse {
    private Integer code;
    private String message;
    private SignItem data;
}