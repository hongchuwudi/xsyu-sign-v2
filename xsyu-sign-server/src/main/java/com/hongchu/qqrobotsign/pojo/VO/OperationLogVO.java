package com.hongchu.qqrobotsign.pojo.VO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogVO {
    private Long id;
    private String logType;
    private String operation;
    private String detail;
    private String result;
    private String operator;
    private Long userId;
    private String username;
    private String userName;
    private String actionMethod;
    private String ip;
    private String requestUri;
    private Long duration;
    private LocalDateTime createdAt;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
