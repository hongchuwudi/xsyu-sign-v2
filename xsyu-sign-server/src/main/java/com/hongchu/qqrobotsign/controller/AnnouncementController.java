package com.hongchu.qqrobotsign.controller;

import com.hongchu.qqrobotsign.pojo.entity.Announcement;
import com.hongchu.qqrobotsign.pojo.DTO.AnnouncementSaveRequest;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.service.IAnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告管理
 *
 * @author hongchu
 * @since 2025-11-18
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AnnouncementController {

    @Autowired private IAnnouncementService announcementService;

    /**
     * 获取最新公告
     *
     * @return 最新公告
     */
    @GetMapping("/user/announcement/latest")
    public Result<Announcement> getLatest() {
        log.info("controller层-获取最新公告");
        Announcement latest = announcementService.getLatest();
        return Result.success(latest);
    }

    /**
     * 获取所有公告
     *
     * @return 公告列表
     */
    @GetMapping("/admin/announcements")
    public Result<List<Announcement>> listAll() {
        log.info("controller层-获取所有公告");
        return Result.success(announcementService.listAll());
    }

    /**
     * 获取单个公告
     *
     * @param id 公告id
     * @return 公告
     */
    @GetMapping("/admin/announcements/{id}")
    public Result<Announcement> getById(@PathVariable Long id) {
        log.info("controller层-获取单个公告-id: {}", id);
        return Result.success(announcementService.getById(id));
    }

    /**
     * 新增公告
     *
     * @param announcement 公告信息
     * @return 操作结果
     */
    @PostMapping("/admin/announcements")
    public Result<Void> add(@RequestBody AnnouncementSaveRequest request) {
        log.info("controller层-新增公告");
        announcementService.add(request);
        return Result.success();
    }

    /**
     * 更新公告
     *
     * @param id           公告id
     * @param announcement 公告信息
     * @return 操作结果
     */
    @PutMapping("/admin/announcements/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody AnnouncementSaveRequest request) {
        log.info("controller层-更新公告-id: {}", id);
        announcementService.update(id, request);
        return Result.success();
    }

    /**
     * 删除公告
     *
     * @param id 公告id
     * @return 操作结果
     */
    @DeleteMapping("/admin/announcements/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("controller层-删除公告-id: {}", id);
        announcementService.delete(id);
        return Result.success();
    }
}
