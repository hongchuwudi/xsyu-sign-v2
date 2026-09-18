package com.hongchu.qqrobotsign.pojo.DTO;

import lombok.Data;

import java.util.List;

@Data
public class EmailNotificationTaskDTO {
    private String subject;
    private String content;
    private List<Long> userIds;
    private List<Long> groupIds;
}
