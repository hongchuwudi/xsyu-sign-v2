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

@RestController
@RequiredArgsConstructor
@Slf4j
public class OperationLogController {

    @Autowired private IOperationLogService operationLogService;

    /** 分页查询日志 */
    @GetMapping("/admin/operation-logs")
    public Result<Page<OperationLogVO>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String logType) {
        return Result.success(operationLogService.queryPage(page, size, startDate, endDate, logType));
    }

    /** 批量删除 */
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
