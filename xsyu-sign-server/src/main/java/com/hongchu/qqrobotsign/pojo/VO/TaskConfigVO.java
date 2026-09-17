package com.hongchu.qqrobotsign.pojo.VO;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务配置VO
 */
@Data
@Builder
public class TaskConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String taskName;
    private String taskKey;
    private String cronExpression;
    private String description;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 解析后的时间配置（用于前端展示）
     */
    private ParsedCron parsedCron;
    
    /**
     * 调度任务配置（完整）
     */
    private ScheduleConfig scheduleConfig;
    
    @Data
    public static class ScheduleConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<String> scheduleDates;
        private Integer scheduleYear;
        private List<String> holidayDates;
        private List<String> autoSelectedDates;
        private Integer hour;
        private Integer minute;
        private Integer delayRange;
    }

    @Data
    @Builder
    public static class ParsedCron implements Serializable {
        private static final long serialVersionUID = 1L;

        // 调度任务专用
        private String daysOfWeek;  // 执行的星期
        private String hour;        // 小时
        private String minute;      // 分钟

        // 间隔执行专用
        private String startHour;   // 开始时
        private String startMinute;  // 开始分钟
        private String endHour;     // 结束小时
        private String endMinute;    // 结束分钟
        private String interval;    // 间隔分钟

        // JWS刷新专用
        private String jwsHour;       // JWS小时
        private String jwsMinute;     // JWS分钟
        private String jwsStartDate;  // JWS起始日期
        private Integer jwsIntervalWeeks; // JWS间隔周数
        private String jwsNextRefresh; // 下次刷新日期
    }
}
