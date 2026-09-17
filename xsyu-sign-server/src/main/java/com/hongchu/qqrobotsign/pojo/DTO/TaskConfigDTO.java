package com.hongchu.qqrobotsign.pojo.DTO;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 定时任务配置DTO
 */
@Data
public class TaskConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务标识
     */
    private String taskKey;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 调度任务配置
     */
    private ScheduleConfig scheduleConfig;

    /**
     * 间隔执行配置
     */
    private IntervalConfig intervalConfig;

    /**
     * JWS刷新配置
     */
    private JwsConfig jwsConfig;

    @Data
    public static class ScheduleConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<String> scheduleDates; // ["2026-03-02",...]
        private Integer scheduleYear;
        private Integer hour;
        private Integer minute;
        private Integer delayRange; // 延迟范围（分钟）
    }

    @Data
    public static class IntervalConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer startHour;
        private Integer startMinute;
        private Integer endHour;
        private Integer endMinute;
        private Integer intervalMinutes;
        // 星期选择
        private String daysOfWeek;
    }

    @Data
    public static class JwsConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        private String startDate;    // "2026-05-04"
        private Integer intervalWeeks; // 1-4
        private Integer hour;
        private Integer minute;
    }
}
