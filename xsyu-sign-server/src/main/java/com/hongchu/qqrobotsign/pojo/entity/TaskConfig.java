package com.hongchu.qqrobotsign.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 定时任务配置实体
 */
@Data
@TableName("task_config")
public class TaskConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务标识
     */
    private String taskKey;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 延迟范围（分钟）- 仅用于调度任务
     */
    private Integer delayRange;

    /**
     * 日历年份 - 调度任务使用
     */
    private Integer scheduleYear;

    /**
     * 签到日期JSON数组 - 调度任务使用
     */
    private String scheduleDates;

    /**
     * JWS刷新起始日期
     */
    private LocalDate jwsStartDate;

    /**
     * JWS刷新间隔周数(1-4)
     */
    private Integer jwsIntervalWeeks;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
