package com.hongchu.qqrobotsign.pojo.VO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailNotificationTaskVO {
    private Long id;
    private String subject;
    private String content;
    private String status;
    private Boolean enabled;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer totalCount;
    private Integer successCount;
    private Integer failureCount;
    private LocalDateTime createdAt;
}
