package com.hongchu.qqrobotsign.pojo.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {
    private String username;
    private String password;
    private String name;
    private String email;
    private Boolean autoSign;
    private String signDays;
    private String signStartTime;
    private String signEndTime;
}

