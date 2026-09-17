package com.hongchu.qqrobotsign.pojo.VO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLoginVO {
    Long id;
    String name;
    String username;
    String email;
    String jwt;
    private Boolean autoSign;
    private String signDays;
    private String role; // 角色：ADMIN 或 USER
    private String signStartTime;
    private String signEndTime;
    private String phone;   // 绑定手机号（短信登录时记录）
}
