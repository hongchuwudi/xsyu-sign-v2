package com.hongchu.qqrobotsign.content;

import java.time.LocalDateTime;

public class EmailContext {

    // 签到时间安排通知模板
    public static final String SCHEDULE_NOTICE_CONTENT = """
            您的签到时间已安排！
            
            用户：%s
            计划签到时间：%s
            签到地点：鄠邑校区
            签到方式：自动签到
            
            系统将在指定时间自动为您完成签到，签到成功会第一时间通知您。
            """;

    // 签到结果通知模板
    public static final String SIGN_RESULT_CONTENT = """
            %s
            
            用户：%s
            执行时间：%s
            执行结果：%s
            
            %s
            """;

    // 续签失败通知模版
    public static final String JWS_REFRESH_ERR_CONTENT = """
            %s
            
            用户：%s
            续签时间：%s
            续签结果：%s
            
            %s
            """;

    public static final String SIGN_SUCCESS_TITLE = "签到成功！";
    public static final String SIGN_SUCCESS_STATUS = "已完成";
    public static final String SIGN_SUCCESS_MESSAGE = "您的签到任务已自动完成。";

    public static final String SIGN_FAIL_TITLE = "签到失败！";
    public static final String SIGN_FAIL_STATUS = "未完成";
    public static final String SIGN_FAIL_PREFIX = "失败原因：";
    public static final String SIGN_FAIL_SUFFIX = "，请及时处理。";

    // 邮件主题
    public static final String SCHEDULE_NOTICE_SUBJECT = "签到时间安排通知";
    public static final String SIGN_SUCCESS_SUBJECT = "签到成功通知";
    public static final String SIGN_FAIL_SUBJECT = "签到失败通知";
    public static final String JWS_REFRESH_SUBJECT = "续签失败";

    // 邮件签名
    public static final String EMAIL_SIGNATURE = """
            \n---
            油签机 - 校园自动签到服务
            网站：xsyusign.hongchu.xyz
            联系作者QQ：2772167017
            """;

}