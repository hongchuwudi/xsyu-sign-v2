package com.hongchu.qqrobotsign.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serial;
import java.time.LocalDateTime;
import java.io.Serializable;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author hongchu
 * @since 2025-11-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 加密密码
     */
    private byte[] password;

    private String name;

    /**
     * 邮箱
     */
    private String email;

    /**
     * JWs令牌
     */
    private String jws;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * JWS续签时间
     */
    private LocalDateTime jwsRefreshedAt;

    /**
     * 自动签到
     */
    private Boolean autoSign;

    /**
     * 签到日期配置（逗号分隔的周几，0=周日，1=周一，...，6=周六）
     * 默认值："0,1,2,3,4,5,6"（每天签到）
     */
    private String signDays;

    /**
     * 签到开始时间
     */
    private LocalTime signStartTime;

    /**
     * 签到结束时间
     */
    private LocalTime signEndTime;
}
