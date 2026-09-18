package com.hongchu.qqrobotsign.pojo.DTO;

import lombok.Data;

@Data
public class AnnouncementSaveRequest {
    private String title;
    private String content;
    private String appVersion;
    private String draftToken;
}
