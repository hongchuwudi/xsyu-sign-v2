package com.hongchu.qqrobotsign.result.res;

import com.hongchu.qqrobotsign.pojo.entity.SignItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignListResponse {
    private Integer code;
    private String message;
    private List<SignItem> data;
}