package com.hongchu.qqrobotsign.service.impl;

import com.hongchu.qqrobotsign.pojo.VO.RedisQueueVO;
import com.hongchu.qqrobotsign.service.IRedisQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class RedisQueueServiceImpl implements IRedisQueueService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String DELAY_QUEUE = "sign:queue";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public RedisQueueVO getQueueInfo() {
        try {
            // 获取队列大小
            Long queueSize = redisTemplate.opsForZSet().size(DELAY_QUEUE);

            // 获取队列中的所有任务（带分数/执行时间）
            Set<ZSetOperations.TypedTuple<Object>> tasks = 
                redisTemplate.opsForZSet().rangeWithScores(DELAY_QUEUE, 0, -1);

            List<RedisQueueVO.QueueTaskVO> taskList = new ArrayList<>();
            if (tasks != null) {
                long currentTime = System.currentTimeMillis();
                for (ZSetOperations.TypedTuple<Object> task : tasks) {
                    String username = (String) task.getValue();
                    Double score = task.getScore();

                    if (username != null && score != null) {
                        long executeTime = score.longValue();
                        long waitSeconds = Math.max(0, (executeTime - currentTime) / 1000);

                        RedisQueueVO.QueueTaskVO taskVO = RedisQueueVO.QueueTaskVO.builder()
                                .username(username)
                                .executeTime(executeTime)
                                .executeTimeFormatted(DATE_FORMAT.format(new Date(executeTime)))
                                .waitSeconds(waitSeconds)
                                .waitTimeFormatted(formatWaitTime(waitSeconds))
                                .build();

                        taskList.add(taskVO);
                    }
                }
            }

            return RedisQueueVO.builder()
                    .queueName(DELAY_QUEUE)
                    .queueSize(queueSize != null ? queueSize : 0L)
                    .tasks(taskList)
                    .build();

        } catch (Exception e) {
            log.error("获取Redis队列信息失败", e);
            return RedisQueueVO.builder()
                    .queueName(DELAY_QUEUE)
                    .queueSize(0L)
                    .tasks(Collections.emptyList())
                    .build();
        }
    }

    @Override
    public void clearQueue() {
        try {
            redisTemplate.delete(DELAY_QUEUE);
            log.info("Redis队列已清空: {}", DELAY_QUEUE);
        } catch (Exception e) {
            log.error("清空Redis队列失败", e);
            throw new RuntimeException("清空队列失败", e);
        }
    }

    /**
     * 格式化等待时间
     */
    private String formatWaitTime(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + "分钟";
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
        } else {
            long days = seconds / 86400;
            long hours = (seconds % 86400) / 3600;
            return days + "天" + (hours > 0 ? hours + "小时" : "");
        }
    }
}
