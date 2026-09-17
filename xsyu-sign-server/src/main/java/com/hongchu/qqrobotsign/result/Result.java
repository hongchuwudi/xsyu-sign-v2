package com.hongchu.qqrobotsign.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一API响应格式
 */
@Data
public class Result<T> implements Serializable {
    
    private Integer code;       // 状态码
    private String message;     // 提示信息
    private T data;             // 响应数据
    private Long timestamp;     // 时间戳
    
    // 私有构造器
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    // 成功响应 - 无数据
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        return result;
    }
    
    // 成功响应 - 有数据
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }
    
    // 失败响应 - 失败响应-返回错误码和错误信息
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    // 失败响应-只返回异常信息
    public static <T> Result<T> fail( String message) {
        Result<T> result = new Result<>();
        result.setCode(666); // 统一错误code
        result.setMessage(message);
        return result;
    }
    //  失败响应 - 只返回错误码
    public static <T> Result<T> fail(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    // 链式调用支持
    public Result<T> code(Integer code) {
        this.code = code;
        return this;
    }
    
    public Result<T> message(String message) {
        this.message = message;
        return this;
    }
    
    public Result<T> data(T data) {
        this.data = data;
        return this;
    }
}