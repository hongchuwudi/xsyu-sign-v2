package com.hongchu.qqrobotsign.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hongchu.qqrobotsign.pojo.entity.Announcement;

import java.util.List;

public interface IAnnouncementService extends IService<Announcement> {

    /** 获取最新公告 */
    Announcement getLatest();

    /** 获取所有公告列表 */
    List<Announcement> listAll();

    /** 根据ID获取 */
    Announcement getById(Long id);

    /** 新增公告 */
    void add(Announcement announcement);

    /** 更新公告 */
    void update(Announcement announcement);

    /** 删除公告 */
    void delete(Long id);
}
