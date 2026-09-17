package com.hongchu.qqrobotsign.result;

import lombok.Getter;

/**
 * 响应状态码枚举
 */
@Getter
public enum ResultCode {
    
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "访问禁止"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_SERVER_ERROR(500, "系统内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    
    // 业务错误码 1000-1999
    BUSINESS_ERROR(1000, "业务异常"),
    PARAM_VALID_ERROR(1001, "参数校验失败"),
    DATA_NOT_EXIST(1002, "数据不存在"),
    DATA_EXISTED(1003, "数据已存在"),
    
    // 代理相关错误码 2000-2999
    PROXY_ERROR(2000, "代理服务异常"),
    PROXY_TIMEOUT(2001, "代理请求超时"),
    PROXY_TARGET_ERROR(2002, "目标服务异常");
    
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}