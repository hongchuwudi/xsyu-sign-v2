package com.hongchu.qqrobotsign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hongchu.qqrobotsign.exception.BusinessException;
import com.hongchu.qqrobotsign.mapper.AnnouncementMapper;
import com.hongchu.qqrobotsign.pojo.entity.Announcement;
import com.hongchu.qqrobotsign.pojo.DTO.AnnouncementSaveRequest;
import com.hongchu.qqrobotsign.service.MediaAssetService;
import com.hongchu.qqrobotsign.service.IAnnouncementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements IAnnouncementService {

    private final MediaAssetService mediaAssetService;

    public AnnouncementServiceImpl(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    @Override
    public Announcement getLatest() {
        return lambdaQuery().orderByDesc(Announcement::getCreatedAt).last("LIMIT 1").one();
    }

    @Override
    public List<Announcement> listAll() {
        return lambdaQuery().orderByDesc(Announcement::getCreatedAt).list();
    }

    @Override
    public Announcement getById(Long id) {
        Announcement a = super.getById(id);
        if (a == null) throw new BusinessException("公告不存在");
        return a;
    }

    @Override
    @Transactional
    public void add(AnnouncementSaveRequest request) {
        Announcement announcement = new Announcement();
        announcement.setTitle(require(request.getTitle(), "公告标题不能为空"));
        announcement.setContent(require(request.getContent(), "公告内容不能为空"));
        announcement.setAppVersion(request.getAppVersion());
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setUpdatedAt(LocalDateTime.now());
        save(announcement);
        mediaAssetService.syncAnnouncementReferences(announcement.getId(), announcement.getContent(), request.getDraftToken());
        log.info("新增公告: {}", announcement.getTitle());
    }

    @Override
    @Transactional
    public void update(Long id, AnnouncementSaveRequest request) {
        Announcement announcement = getById(id);
        announcement.setTitle(require(request.getTitle(), "公告标题不能为空"));
        announcement.setContent(require(request.getContent(), "公告内容不能为空"));
        announcement.setAppVersion(request.getAppVersion());
        announcement.setUpdatedAt(LocalDateTime.now());
        updateById(announcement);
        mediaAssetService.syncAnnouncementReferences(id, announcement.getContent(), request.getDraftToken());
        log.info("更新公告: id={}", id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        mediaAssetService.markAnnouncementOrphaned(id);
        removeById(id);
        log.info("删除公告: id={}", id);
    }

    private String require(String value, String message) {
        if (value == null || value.isBlank()) throw new BusinessException(message);
        return value.trim();
    }
}
