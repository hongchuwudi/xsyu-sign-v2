package com.hongchu.qqrobotsign.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_asset_ref")
public class ContentAssetRef {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String bizType;
    private Long bizId;
    private Long assetId;
    private LocalDateTime createdAt;
}
