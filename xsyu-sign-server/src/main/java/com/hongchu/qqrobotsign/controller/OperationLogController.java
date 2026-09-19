package com.hongchu.qqrobotsign.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongchu.qqrobotsign.pojo.VO.OperationLogVO;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.service.IOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 管理员操作日志管理。 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class OperationLogController {

    private final IOperationLogService operationLogService;

    @GetMapping("/admin/operation-logs")
    public Result<Page<OperationLogVO>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String actionMethod) {
        log.info("分页查询操作日志-page: {}, size: {}, startDate: {}, endDate: {}, logType: {}, result: {}, actionMethod: {}",
                page, size, startDate, endDate, logType, result, actionMethod);
        return Result.success(operationLogService.queryPage(
                page, size, startDate, endDate, logType, keyword, result, actionMethod));
    }

    @DeleteMapping("/admin/operation-logs")
    public Result<Void> deleteByIds(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.fail("ids不能为空");
        }
        operationLogService.deleteByIds(ids);
        return Result.success();
    }
}
