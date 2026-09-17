package com.hongchu.qqrobotsign.service;

import com.hongchu.qqrobotsign.pojo.VO.RedisQueueVO;

/**
 * Redis 队列服务接口
 */
public interface IRedisQueueService {
    /**
     * 获取队列信息
     * @return 队列信息
     */
    RedisQueueVO getQueueInfo();

    /**
     * 清空队列
     */
    void clearQueue();
}
