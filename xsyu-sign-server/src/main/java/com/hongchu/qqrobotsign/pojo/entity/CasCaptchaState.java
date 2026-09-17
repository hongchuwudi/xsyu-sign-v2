package com.hongchu.qqrobotsign.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CasCaptchaState implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String encryptedPassword;
    private String execution;
    private List<CookieEntry> cookies;
    private long createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CookieEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String value;
    }
}
