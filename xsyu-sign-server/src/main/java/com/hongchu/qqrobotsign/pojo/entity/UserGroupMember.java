package com.hongchu.qqrobotsign.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_group_member")
public class UserGroupMember implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    private LocalDateTime createdAt;
}
