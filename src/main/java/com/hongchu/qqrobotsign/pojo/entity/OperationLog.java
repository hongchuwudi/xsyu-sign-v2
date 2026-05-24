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

    /** 请求IP */
    private String ip;

    /** 执行耗时(ms) */
    private Long duration;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
