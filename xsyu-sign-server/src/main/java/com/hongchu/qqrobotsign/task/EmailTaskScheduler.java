package com.hongchu.qqrobotsign.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hongchu.qqrobotsign.mapper.EmailNotificationTaskMapper;
import com.hongchu.qqrobotsign.pojo.entity.EmailNotificationTask;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EmailTaskScheduler {
    private final EmailNotificationTaskMapper taskMapper;
    private final EmailTaskDispatcher taskDispatcher;

    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void dispatchDueTasks() {
        List<EmailNotificationTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<EmailNotificationTask>()
                        .select(EmailNotificationTask::getId)
                        .eq(EmailNotificationTask::getStatus, "PENDING")
                        .eq(EmailNotificationTask::getEnabled, true)
                        .and(wrapper -> wrapper.isNull(EmailNotificationTask::getScheduledAt)
                                .or().le(EmailNotificationTask::getScheduledAt, LocalDateTime.now()))
                        .orderByAsc(EmailNotificationTask::getScheduledAt)
                        .last("LIMIT 20"));
        tasks.forEach(task -> taskDispatcher.dispatchAsync(task.getId()));
    }
}
