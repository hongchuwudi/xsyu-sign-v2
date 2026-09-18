package com.hongchu.qqrobotsign.controller;

import com.hongchu.qqrobotsign.pojo.VO.MediaAssetUploadVO;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.service.MediaAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/assets")
@RequiredArgsConstructor
public class MediaAssetController {
    private final MediaAssetService mediaAssetService;

    @PostMapping("/images")
    public Result<MediaAssetUploadVO> uploadImage(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "draftToken", required = false) String draftToken) {
        return Result.success(mediaAssetService.upload(file, draftToken));
    }
}
