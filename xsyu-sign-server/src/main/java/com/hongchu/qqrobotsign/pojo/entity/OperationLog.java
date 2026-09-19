package com.hongchu.qqrobotsign.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 日志类型: API / SCHEDULE / INTERVAL_SIGN / JWS_REFRESH */
    private String logType;

    /** 操作描述 */
    private String operation;

    /** 详细信息 */
    private String detail;

    /** 执行结果: SUCCESS / FAIL / PARTIAL */
    private String result;

    /** 操作人 */
    private String operator;

    /** 目标用户ID快照 */
    private Long userId;

    /** 目标学号或用户名快照 */
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /** 目标用户姓名快照 */
    private String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /** 操作方式: PASSWORD / SMS / QR / MANUAL / ADMIN */
    private String actionMethod;

    /** 请求IP */
    private String ip;

    /** 请求路径 */
    private String requestUri;

    /** 执行耗时(ms) */
    private Long duration;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
