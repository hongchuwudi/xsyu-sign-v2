package com.hongchu.qqrobotsign.service;

import com.hongchu.qqrobotsign.pojo.VO.MediaAssetUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface MediaAssetService {
    MediaAssetUploadVO upload(MultipartFile file, String draftToken);
    void syncAnnouncementReferences(Long announcementId, String markdown, String draftToken);
    void markAnnouncementOrphaned(Long announcementId);
}
