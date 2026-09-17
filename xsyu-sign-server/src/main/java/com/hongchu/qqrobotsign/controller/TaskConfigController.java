package com.hongchu.qqrobotsign.controller;

import com.hongchu.qqrobotsign.pojo.DTO.TaskConfigDTO;
import com.hongchu.qqrobotsign.pojo.VO.TaskConfigVO;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.service.ITaskConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 管理员-定时任务配置管理 前端控制器
 * </p>
 *
 * @author hongchu
 * @since 2025-11-23
 */
@RestController
@RequestMapping("/admin/task-config")
@RequiredArgsConstructor
@Slf4j
public class TaskConfigController {

    @Autowired private ITaskConfigService taskConfigService;

    /**
     * 获取所有任务配置
     *
     * @return 任务配置列表
     */
    @GetMapping
    public Result<List<TaskConfigVO>> getAllTaskConfigs() {
        log.info("controller层-获取所有任务配置");
        List<TaskConfigVO> list = taskConfigService.getAllTaskConfigs();
        return Result.success(list);
    }

    /**
     * 根据任务标识获取配置
     *
     * @param taskKey 任务标识
     * @return 任务配置
     */
    @GetMapping("/{taskKey}")
    public Result<TaskConfigVO> getTaskConfigByKey(@PathVariable String taskKey) {
        log.info("controller层-获取任务配置-taskKey: {}", taskKey);
        TaskConfigVO vo = taskConfigService.getTaskConfigByKey(taskKey);
        if (vo == null) {
            return Result.fail("任务配置不存在");
        }
        return Result.success(vo);
    }

    /**
     * 更新任务配置
     *
     * @param taskKey 任务标识
     * @param dto     任务配置信息
     * @return 更新后的任务配置
     */
    @PutMapping("/{taskKey}")
    public Result<TaskConfigVO> updateTaskConfig(@PathVariable String taskKey, @RequestBody TaskConfigDTO dto) {
        log.info("controller层-更新任务配置-taskKey: {}", taskKey);
        TaskConfigVO vo = taskConfigService.updateTaskConfig(taskKey, dto);
        return Result.success(vo);
    }

    /**
     * 初始化默认配置
     *
     * @return 操作结果
     */
    @PostMapping("/init")
    public Result<Void> initDefaultConfigs() {
        log.info("controller层-初始化默认任务配置");
        taskConfigService.initDefaultConfigs();
        return Result.success();
    }

    /**
     * 立即执行调度所有用户任务
     *
     * @param sendEmail 是否发送邮件
     * @return 操作结果
     */
    @PostMapping("/schedule-users/immediate")
    public Result<Void> triggerImmediateSchedule(@RequestParam(defaultValue = "true") boolean sendEmail) {
        log.info("controller层-立即执行调度所有用户任务, sendEmail: {}", sendEmail);
        taskConfigService.triggerImmediateSchedule(sendEmail);
        return Result.success();
    }

    /**
     * 获取调度日历数据
     *
     * @param year 年份
     * @return 调度日历数据
     */
    @GetMapping("/schedule-users/calendar")
    public Result<Map<String, Object>> getScheduleCalendar(@RequestParam(defaultValue = "2026") int year) {
        log.info("controller层-获取调度日历-year: {}", year);
        Map<String, Object> calendar = taskConfigService.getScheduleCalendar(year);
        return Result.success(calendar);
    }
}