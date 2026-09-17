package com.hongchu.qqrobotsign.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CasQrState implements Serializable {
    private static final long serialVersionUID = 1L;

    private String loginLt;
    private String username;
    private List<CasCaptchaState.CookieEntry> cookies;
    private long createdAt;
}
