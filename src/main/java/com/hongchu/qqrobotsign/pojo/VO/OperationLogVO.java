package com.hongchu.qqrobotsign.pojo.VO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogVO {
    private Long id;
    private String logType;
    private String operation;
    private String detail;
    private String result;
    private String operator;
    private String ip;
    private Long duration;
    private LocalDateTime createdAt;
}
