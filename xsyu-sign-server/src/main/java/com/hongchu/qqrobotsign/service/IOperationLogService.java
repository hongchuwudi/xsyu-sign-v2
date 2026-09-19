package com.hongchu.qqrobotsign.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongchu.qqrobotsign.pojo.VO.OperationLogVO;

import java.time.LocalDate;
import java.util.List;

public interface IOperationLogService {

    /** 写入日志 */
    void save(String logType, String operation, String detail, String result, String operator, String ip, Long duration);

    /** 写入包含用户与请求上下文的详细日志 */
    void saveDetailed(String logType, String operation, String detail, String result, String operator,
                      Long userId, String username, String userName, String actionMethod,
                      String ip, String requestUri, Long duration);

    /** 分页查询，支持日期范围和类型筛选 */
    Page<OperationLogVO> queryPage(int page, int size, LocalDate startDate, LocalDate endDate,
                                   String logType, String keyword, String result, String actionMethod);

    /** 批量删除 */
    void deleteByIds(List<Long> ids);
}
