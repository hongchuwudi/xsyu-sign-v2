package com.hongchu.qqrobotsign.pojo.VO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailTaskRecipientVO {
    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String email;
    private String status;
    private String errorMessage;
    private LocalDateTime sentAt;
}
