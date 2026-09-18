package com.hongchu.qqrobotsign.pojo.VO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MediaAssetUploadVO {
    private Long assetId;
    private String url;
    private String originalName;
    private String contentType;
    private Long fileSize;
}
