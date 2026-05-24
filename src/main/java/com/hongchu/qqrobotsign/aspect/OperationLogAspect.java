package com.hongchu.qqrobotsign.aspect;

import com.hongchu.qqrobotsign.annotation.LogRecord;
import com.hongchu.qqrobotsign.service.IOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Autowired private IOperationLogService operationLogService;

    @Around("@annotation(com.hongchu.qqrobotsign.annotation.LogRecord)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String result = "SUCCESS";
        String detail = null;
        try {
            Object ret = pjp.proceed();
            return ret;
        } catch (Throwable e) {
            result = "FAIL";
            detail = e.getMessage();
            if (detail != null && detail.length() > 500) {
                detail = detail.substring(0, 500);
            }
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            LogRecord anno = sig.getMethod().getAnnotation(LogRecord.class);
            String operation = anno.value();

            String ip = "unknown";
            String operator = "ANONYMOUS";
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest req = attrs.getRequest();
                    ip = req.getRemoteAddr();
                    String jwt = req.getHeader("Authorization");
                    if (jwt != null && jwt.startsWith("Bearer ")) {
                        operator = jwt.substring(7);
                        if (operator.length() > 30) operator = operator.substring(0, 30) + "...";
                    }
                }
            } catch (Exception ignored) {
            }

            try {
                operationLogService.save("API", operation, detail, result, operator, ip, duration);
            } catch (Exception e) {
                log.error("记录操作日志失败", e);
            }
        }
    }
}
