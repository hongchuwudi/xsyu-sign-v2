package com.hongchu.qqrobotsign.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hongchu.qqrobotsign.pojo.DTO.TaskConfigDTO;
import com.hongchu.qqrobotsign.pojo.VO.TaskConfigVO;
import com.hongchu.qqrobotsign.pojo.entity.TaskConfig;

import java.util.List;
import java.util.Map;

/**
 * 定时任务配置服务接口
 */
public interface ITaskConfigService extends IService<TaskConfig> {

    /**
     * 获取所有任务配置
     */
    List<TaskConfigVO> getAllTaskConfigs();

    /**
     * 根据任务标识获取配置
     */
    TaskConfigVO getTaskConfigByKey(String taskKey);

    /**
     * 更新任务配置
     */
    TaskConfigVO updateTaskConfig(String taskKey, TaskConfigDTO dto);

    /**
     * 初始化默认配置
     */
    void initDefaultConfigs();

    /**
     * 解析Cron表达式为可读格式
     */
    TaskConfigVO.ParsedCron parseCronExpression(String taskKey, String cron);

    /**
     * 构建Cron表达式
     */
    String buildCronExpression(String taskKey, TaskConfigDTO dto);

    /**
     * 立即执行调度所有用户任务
     */
    void triggerImmediateSchedule(boolean sendEmail);

    /**
     * 获取某年的调度日历数据
     */
    Map<String, Object> getScheduleCalendar(int year);
}
