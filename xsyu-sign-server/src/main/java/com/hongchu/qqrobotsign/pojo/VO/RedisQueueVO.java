package com.hongchu.qqrobotsign.pojo.VO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Redis 队列信息 VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RedisQueueVO {
    /**
     * 队列名称
     */
    private String queueName;

    /**
     * 队列中的任务数量
     */
    private Long queueSize;

    /**
     * 队列中的任务列表
     */
    private List<QueueTaskVO> tasks;

    /**
     * 队列任务 VO
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class QueueTaskVO {
        /**
         * 用户名
         */
        private String username;

        /**
         * 执行时间戳
         */
        private Long executeTime;

        /**
         * 执行时间（格式化）
         */
        private String executeTimeFormatted;

        /**
         * 等待时长（秒）
         */
        private Long waitSeconds;

        /**
         * 等待时长（格式化）
         */
        private String waitTimeFormatted;
    }
}
