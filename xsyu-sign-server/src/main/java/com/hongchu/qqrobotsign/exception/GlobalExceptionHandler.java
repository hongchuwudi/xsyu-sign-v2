package com.hongchu.qqrobotsign.exception;

import com.hongchu.qqrobotsign.pojo.VO.CasCaptchaVO;
import com.hongchu.qqrobotsign.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获验证码异常
     */
    @ExceptionHandler(CaptchaRequiredException.class)
    public Result<CasCaptchaVO> handleCaptchaRequired(CaptchaRequiredException ex) {
        log.warn("CAS登录需要验证码: sessionId={}", ex.getCaptchaSessionId());
        CasCaptchaVO vo = CasCaptchaVO.builder()
                .captchaSessionId(ex.getCaptchaSessionId())
                .captchaImageBase64(ex.getCaptchaImageBase64())
                .expiresIn(ex.getExpiresIn())
                .message(ex.getMessage())
                .build();
        return Result.<CasCaptchaVO>fail(1004, ex.getMessage()).data(vo);
    }

    /**
     * 捕获业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<String> exceptionHandler(BusinessException ex){
        log.error("业务异常信息：{}", ex.getMessage());
        return Result.fail(ex.getMessage());
    }

    /**
     * 捕获SQL唯一约束异常
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        String message = ex.getMessage();
        log.error("SQL异常信息：{}", message);

        if(message.contains("Duplicate entry")){
            String[] split = message.split(" ");
            String username = split[2];
            String msg = username + "早已存在";
            return Result.fail(msg);
        }else{
            return Result.fail("数据库操作失败");
        }
    }

    /**
     * 捕获 @RequestBody 参数校验异常（@Valid 校验失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.error("参数校验异常：{}", msg);
        return Result.fail(msg);
    }

    /**
     * 捕获表单参数绑定校验异常
     */
    @ExceptionHandler(BindException.class)
    public Result<String> handleBindException(BindException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        log.error("参数绑定异常：{}", msg);
        return Result.fail(msg);
    }

    /**
     * 捕获缺少必填请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<String> handleMissingParam(MissingServletRequestParameterException ex) {
        String msg = "缺少必填参数：" + ex.getParameterName();
        log.error("缺少请求参数：{}", msg);
        return Result.fail(msg);
    }

    /**
     * 捕获请求参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "参数类型不匹配：" + ex.getName();
        log.error("参数类型不匹配：{}", msg);
        return Result.fail(msg);
    }

    /**
     * 捕获请求体格式错误异常（如 JSON 解析失败）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<String> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("请求体解析异常：{}", ex.getMessage());
        return Result.fail("请求体格式错误");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<String> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("上传文件超过大小限制");
        return Result.fail("图片超过上传大小限制");
    }

    /**
     * 捕获请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<String> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.error("请求方法不支持：{}", ex.getMessage());
        return Result.fail("请求方法不支持：" + ex.getMethod());
    }

    /**
     * 捕获 404 异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<String> handleNoHandlerFound(NoHandlerFoundException ex) {
        log.error("接口不存在：{}", ex.getRequestURL());
        return Result.fail("接口不存在：" + ex.getRequestURL());
    }

    /**
     * 捕获所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<String> exceptionHandler(Exception ex){
        log.error("系统异常信息：", ex);
        return Result.fail("系统繁忙，请稍后重试");
    }
}
