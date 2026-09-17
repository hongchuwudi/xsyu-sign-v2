package com.hongchu.qqrobotsign.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("email_notification_task")
public class EmailNotificationTask implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
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
    private LocalDateTime updatedAt;
}
