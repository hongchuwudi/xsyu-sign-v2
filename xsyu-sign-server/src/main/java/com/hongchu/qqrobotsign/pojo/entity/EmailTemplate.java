package com.hongchu.qqrobotsign.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("email_template")
public class EmailTemplate implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    private String subject;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
