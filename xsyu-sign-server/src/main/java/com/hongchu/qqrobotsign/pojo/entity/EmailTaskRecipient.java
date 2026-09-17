package com.hongchu.qqrobotsign.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("email_task_recipient")
public class EmailTaskRecipient implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long userId;
    private String username;
    private String name;
    private String email;
    private String status;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
