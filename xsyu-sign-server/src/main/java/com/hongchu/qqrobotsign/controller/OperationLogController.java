package com.hongchu.qqrobotsign.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongchu.qqrobotsign.pojo.VO.OperationLogVO;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.service.IOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 操作日志 前端控制器
 * </p>
 *
 * @author hongchu
 * @since 2025-11-18
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class OperationLogController {

    @Autowired private IOperationLogService operationLogService;

    /**
     * 分页查询日志
     *
     * @param page      页码
     * @param size      每页数量
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param logType   日志类型
     * @return 分页日志结果
     */
    @GetMapping("/admin/operation-logs")
    public Result<Page<OperationLogVO>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String logType) {
        log.info("controller层-分页查询操作日志-page: {}, size: {}, startDate: {}, endDate: {}, logType: {}",
                page, size, startDate, endDate, logType);
        return Result.success(operationLogService.queryPage(page, size, startDate, endDate, logType));
    }

    /**
     * 批量删除日志
     *
     * @param body 包含 ids 列表的请求体
     * @return 删除结果
     */
    @DeleteMapping("/admin/operation-logs")
    public Result<Void> deleteByIds(@RequestBody Map<String, List<Long>> body) {
        log.info("controller层-批量删除操作日志");
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) return Result.fail("ids不能为空");
        operationLogService.deleteByIds(ids);
        return Result.success();
    }
}