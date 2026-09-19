package com.hongchu.qqrobotsign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hongchu.qqrobotsign.mapper.OperationLogMapper;
import com.hongchu.qqrobotsign.pojo.VO.OperationLogVO;
import com.hongchu.qqrobotsign.pojo.entity.OperationLog;
import com.hongchu.qqrobotsign.service.IOperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements IOperationLogService {

    @Override
    public void save(String logType, String operation, String detail, String result, String operator, String ip, Long duration) {
        saveDetailed(logType, operation, detail, result, operator,
                null, null, null, null, ip, null, duration);
    }

    @Override
    public void saveDetailed(String logType, String operation, String detail, String result, String operator,
                             Long userId, String username, String userName, String actionMethod,
                             String ip, String requestUri, Long duration) {
        OperationLog log = new OperationLog();
        log.setLogType(logType);
        log.setOperation(operation);
        log.setDetail(detail);
        log.setResult(result);
        log.setOperator(operator != null ? operator : "SYSTEM");
        log.setUserId(userId);
        log.setUsername(username);
        log.setUserName(userName);
        log.setActionMethod(actionMethod);
        log.setIp(ip);
        log.setRequestUri(requestUri);
        log.setDuration(duration != null ? duration : 0L);
        log.setCreatedAt(LocalDateTime.now());
        save(log);
    }

    @Override
    public Page<OperationLogVO> queryPage(int page, int size, LocalDate startDate, LocalDate endDate,
                                          String logType, String keyword, String result, String actionMethod) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (logType != null && !logType.isBlank()) {
            wrapper.eq(OperationLog::getLogType, logType);
        }
        if (result != null && !result.isBlank()) {
            wrapper.eq(OperationLog::getResult, result);
        }
        if (actionMethod != null && !actionMethod.isBlank()) {
            wrapper.eq(OperationLog::getActionMethod, actionMethod);
        }
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            wrapper.and(query -> query
                    .like(OperationLog::getUsername, value)
                    .or().like(OperationLog::getUserName, value)
                    .or().like(OperationLog::getOperation, value)
                    .or().like(OperationLog::getDetail, value)
                    .or().like(OperationLog::getIp, value));
        }
        if (startDate != null) {
            wrapper.ge(OperationLog::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(OperationLog::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);

        Page<OperationLog> pageResult = page(new Page<>(page, size), wrapper);
        Page<OperationLogVO> voPage = new Page<>(page, size, pageResult.getTotal());
        voPage.setRecords(pageResult.getRecords().stream().map(e -> {
            OperationLogVO vo = new OperationLogVO();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        removeBatchByIds(ids);
    }
}
