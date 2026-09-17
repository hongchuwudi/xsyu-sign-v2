package com.hongchu.qqrobotsign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hongchu.qqrobotsign.exception.BusinessException;
import com.hongchu.qqrobotsign.mapper.AnnouncementMapper;
import com.hongchu.qqrobotsign.pojo.entity.Announcement;
import com.hongchu.qqrobotsign.service.IAnnouncementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements IAnnouncementService {

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
    public void add(Announcement announcement) {
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setUpdatedAt(LocalDateTime.now());
        save(announcement);
        log.info("新增公告: {}", announcement.getTitle());
    }

    @Override
    public void update(Announcement announcement) {
        announcement.setUpdatedAt(LocalDateTime.now());
        updateById(announcement);
        log.info("更新公告: id={}", announcement.getId());
    }

    @Override
    public void delete(Long id) {
        removeById(id);
        log.info("删除公告: id={}", id);
    }
}
