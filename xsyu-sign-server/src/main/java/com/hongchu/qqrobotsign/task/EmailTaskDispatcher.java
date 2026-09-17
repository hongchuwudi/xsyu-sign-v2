package com.hongchu.qqrobotsign.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hongchu.qqrobotsign.mapper.EmailNotificationTaskMapper;
import com.hongchu.qqrobotsign.mapper.EmailTaskRecipientMapper;
import com.hongchu.qqrobotsign.pojo.entity.EmailNotificationTask;
import com.hongchu.qqrobotsign.pojo.entity.EmailTaskRecipient;
import com.hongchu.qqrobotsign.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailTaskDispatcher {
    private final EmailNotificationTaskMapper taskMapper;
    private final EmailTaskRecipientMapper recipientMapper;
    private final EmailService emailService;

    @Async
    public void dispatchAsync(Long taskId) {
        LocalDateTime startedAt = LocalDateTime.now();
        int claimed = taskMapper.update(null, new LambdaUpdateWrapper<EmailNotificationTask>()
                .eq(EmailNotificationTask::getId, taskId)
                .eq(EmailNotificationTask::getStatus, "PENDING")
                .eq(EmailNotificationTask::getEnabled, true)
                .set(EmailNotificationTask::getStatus, "SENDING")
                .set(EmailNotificationTask::getStartedAt, startedAt)
                .set(EmailNotificationTask::getUpdatedAt, startedAt));
        if (claimed == 0) return;

        try {
            EmailNotificationTask task = taskMapper.selectById(taskId);
            List<EmailTaskRecipient> recipients = recipientMapper.selectList(
                    new LambdaQueryWrapper<EmailTaskRecipient>()
                            .eq(EmailTaskRecipient::getTaskId, taskId)
                            .eq(EmailTaskRecipient::getStatus, "PENDING")
                            .orderByAsc(EmailTaskRecipient::getId));
            int successCount = 0;
            int failureCount = 0;
            for (EmailTaskRecipient recipient : recipients) {
                try {
                    emailService.sendManagedEmail(
                            recipient.getEmail(),
                            render(task.getSubject(), recipient),
                            render(task.getContent(), recipient));
                    recipient.setStatus("SUCCESS");
                    recipient.setSentAt(LocalDateTime.now());
                    recipient.setErrorMessage(null);
                    successCount++;
                } catch (Exception exception) {
                    recipient.setStatus("FAILED");
                    recipient.setErrorMessage(limitError(exception));
                    failureCount++;
                    log.error("邮件任务 {} 向 {} 发送失败", taskId, recipient.getEmail(), exception);
                }
                recipientMapper.updateById(recipient);
            }

            LocalDateTime completedAt = LocalDateTime.now();
            taskMapper.update(null, new LambdaUpdateWrapper<EmailNotificationTask>()
                    .eq(EmailNotificationTask::getId, taskId)
                    .set(EmailNotificationTask::getStatus, "COMPLETED")
                    .set(EmailNotificationTask::getEnabled, false)
                    .set(EmailNotificationTask::getSuccessCount, successCount)
                    .set(EmailNotificationTask::getFailureCount, failureCount)
                    .set(EmailNotificationTask::getCompletedAt, completedAt)
                    .set(EmailNotificationTask::getUpdatedAt, completedAt));
            log.info("邮件任务 {} 执行完成，成功 {}，失败 {}", taskId, successCount, failureCount);
        } catch (Exception exception) {
            LocalDateTime failedAt = LocalDateTime.now();
            taskMapper.update(null, new LambdaUpdateWrapper<EmailNotificationTask>()
                    .eq(EmailNotificationTask::getId, taskId)
                    .set(EmailNotificationTask::getStatus, "FAILED")
                    .set(EmailNotificationTask::getEnabled, false)
                    .set(EmailNotificationTask::getCompletedAt, failedAt)
                    .set(EmailNotificationTask::getUpdatedAt, failedAt));
            log.error("邮件任务 {} 执行异常", taskId, exception);
        }
    }

    private String render(String source, EmailTaskRecipient recipient) {
        return source
                .replace("{{name}}", valueOrEmpty(recipient.getName()))
                .replace("{{username}}", valueOrEmpty(recipient.getUsername()))
                .replace("{{email}}", valueOrEmpty(recipient.getEmail()));
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String limitError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) message = exception.getClass().getSimpleName();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
