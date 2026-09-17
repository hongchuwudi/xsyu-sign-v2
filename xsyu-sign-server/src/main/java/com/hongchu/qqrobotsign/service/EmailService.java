package com.hongchu.qqrobotsign.service;

public interface EmailService {
    // 发送简单邮件
    void sendSimpleEmail(String to, String subject, String content);

    // 通知用户签到时间安排
    void sendScheduleNotice(String to, String username, String scheduledTime);

    // 签到结果通知
    void sendSignResultNotice(String to, String username, boolean success, String resultMessage);

    // 发送续签失败通知
    void sendErrorJwsRefreshMes(String to,String username);
}
