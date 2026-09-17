package com.hongchu.qqrobotsign.aspect;

import com.hongchu.qqrobotsign.annotation.LogRecord;
import com.hongchu.qqrobotsign.context.BaseContext;
import com.hongchu.qqrobotsign.pojo.DTO.BindPasswordDTO;
import com.hongchu.qqrobotsign.pojo.DTO.QrCreateDTO;
import com.hongchu.qqrobotsign.pojo.DTO.SmsVerifyDTO;
import com.hongchu.qqrobotsign.pojo.DTO.UserDTO;
import com.hongchu.qqrobotsign.pojo.VO.UserLoginVO;
import com.hongchu.qqrobotsign.result.Result;
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

import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Map;

@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Autowired private IOperationLogService operationLogService;
    @Around("@annotation(com.hongchu.qqrobotsign.annotation.LogRecord)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String result = "SUCCESS";
        String errDetail = null;
        try {
            Object ret = pjp.proceed();
            errDetail = extractSuccessDetail(ret);
            return ret;
        } catch (Throwable e) {
            result = "FAIL";
            errDetail = e.getMessage();
            if (errDetail != null && errDetail.length() > 500) {
                errDetail = errDetail.substring(0, 500);
            }
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            LogRecord anno = sig.getMethod().getAnnotation(LogRecord.class);
            String operation = anno.value();
            String ip = getClientIp();
            String operator = extractOperator(pjp, sig);
            String detail = buildDetail(operation, operator, pjp, errDetail);

            try {
                operationLogService.save("API", operation, detail, result, operator, ip, duration);
            } catch (Exception e) {
                log.error("记录操作日志失败", e);
            }
        }
    }

    /** 从方法参数和BaseContext提取操作人 */
    private String extractOperator(ProceedingJoinPoint pjp, MethodSignature sig) {
        Object[] args = pjp.getArgs();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof BindPasswordDTO dto && dto.getUsername() != null) return dto.getUsername();
                if (arg instanceof SmsVerifyDTO dto && dto.getUsername() != null) return dto.getUsername();
                if (arg instanceof QrCreateDTO dto && dto.getUsername() != null) return dto.getUsername();
            }
        }
        // 从 PathVariable 中查找 username
        Parameter[] params = sig.getMethod().getParameters();
        if (args != null && params.length == args.length) {
            for (int i = 0; i < params.length; i++) {
                if (params[i].isAnnotationPresent(org.springframework.web.bind.annotation.PathVariable.class)) {
                    org.springframework.web.bind.annotation.PathVariable pv =
                            params[i].getAnnotation(org.springframework.web.bind.annotation.PathVariable.class);
                    if ("username".equals(pv.value()) && args[i] instanceof String s) {
                        return s;
                    }
                }
            }
        }
        // 从 BaseContext 获取
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId != null) return "UID:" + userId;
        } catch (Exception ignored) {}
        return "ANONYMOUS";
    }

    /** 构建详情字符串 */
    private String buildDetail(String operation, String operator, ProceedingJoinPoint pjp, String resultDetail) {
        StringBuilder sb = new StringBuilder();
        sb.append("操作: ").append(operation);

        Object[] args = pjp.getArgs();
        if (args != null) {
            for (Object arg : args) {
                if (arg == null) continue;
                if (arg instanceof BindPasswordDTO dto && dto.getUsername() != null) {
                    sb.append(" | 学号: ").append(dto.getUsername());
                } else if (arg instanceof SmsVerifyDTO dto && dto.getUsername() != null) {
                    sb.append(" | 学号: ").append(dto.getUsername());
                } else if (arg instanceof UserDTO dto && dto.getUsername() != null) {
                    sb.append(" | 目标: ").append(dto.getUsername());
                }
            }
        }
        if (resultDetail != null && !resultDetail.isEmpty()) {
            sb.append(" | ").append(resultDetail);
        }
        // 如果只有操作名没有其他信息，补充操作人
        if (sb.toString().equals("操作: " + operation) && !"ANONYMOUS".equals(operator))
            sb.append(" | 操作人: ").append(operator);
        String full = sb.toString();
        if (full.length() > 800) full = full.substring(0, 800) + "...";
        return full;
    }

    /** 从返回值提取成功详情 */
    private String extractSuccessDetail(Object ret) {
        if (ret == null) return "完成";
        if (ret instanceof Result<?> r) {
            if (r.getData() instanceof UserLoginVO vo) return "登录成功 | 角色: " + vo.getRole() + " | 用户: " + vo.getUsername();
            if (r.getData() instanceof String s && !s.isEmpty()) return s.length() > 200 ? s.substring(0, 200) + "..." : s;
            if (r.getData() instanceof Map<?,?> m) return "返回 " + m.size() + " 条数据";
            if (r.getData() instanceof Collection<?> c) return "返回 " + c.size() + " 条数据";
            // data 为 null 时，使用 Result 自带的 message（默认为"操作成功"）
            return r.getMessage() != null ? r.getMessage() : "完成";
        }
        if (ret instanceof String s && !s.isEmpty()) return s.length() > 200 ? s.substring(0, 200) + "..." : s;
        return null;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String ip = req.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = req.getHeader("X-Real-IP");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = req.getRemoteAddr();
                if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
                return ip;
            }
        } catch (Exception ignored) {}
        return "unknown";
    }
}
