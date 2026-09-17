package com.hongchu.qqrobotsign.controller;

import com.hongchu.qqrobotsign.pojo.VO.RedisQueueVO;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.service.IRedisQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * Redis 队列 前端控制器
 * </p>
 *
 * @author hongchu
 * @since 2025-11-18
 */
@RestController
@RequestMapping("/admin")
@Slf4j
public class RedisQueueController {

    @Autowired
    private IRedisQueueService redisQueueService;

    /**
     * 获取 Redis 等待签到任务队列的详细信息
     * @return 队列信息
     */
    @GetMapping("/redis-queue")
    public Result<RedisQueueVO> getRedisQueueInfo() {
        log.info("controller层-获取Redis队列信息");
        RedisQueueVO queueInfo = redisQueueService.getQueueInfo();
        return Result.success(queueInfo);
    }

    /**
     * 清空 Redis 队列
     * @return 操作结果
     */
    @DeleteMapping("/redis-queue")
    public Result<Void> clearRedisQueue() {
        log.info("controller层-清空Redis队列");
        redisQueueService.clearQueue();
        return Result.success();
    }
}