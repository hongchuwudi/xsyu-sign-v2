package com.hongchu.qqrobotsign.pojo.VO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserVO {
    Long id;
    String name;
    String username;
    String email;
    private Boolean autoSign;
    private Boolean jws;
    private LocalDateTime updatedAt;
    private String signDays;
    private String signStartTime;
    private String signEndTime;
    private String jwsRefreshedAt;
}
