package com.hongchu.qqrobotsign.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("media_asset")
public class MediaAsset {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String objectKey;
    private String publicUrl;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String fileHash;
    private String status;
    private String draftToken;
    private Long createdBy;
    private LocalDateTime orphanedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
