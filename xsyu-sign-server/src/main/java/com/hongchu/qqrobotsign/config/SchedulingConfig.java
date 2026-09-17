package com.hongchu.qqrobotsign.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * <p>
 * 定时任务配置类
 * </p>
 *
 * @author hongchu
 * @since 2025-11-23
 */
@Configuration
@EnableScheduling
@Slf4j
public class SchedulingConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setRemoveOnCancelPolicy(true);

        // 打印时区信息，排查 cron 时区不一致问题
        log.info("TaskScheduler 初始化 - JVM默认时区: {}, 系统时区: {}",
                java.util.TimeZone.getDefault().getID(),
                java.time.ZoneId.systemDefault());
        log.info("当前服务器时间: {}", java.time.LocalDateTime.now());

        return scheduler;
    }
}
