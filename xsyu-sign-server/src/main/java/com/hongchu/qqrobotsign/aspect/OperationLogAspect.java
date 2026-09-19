package com.hongchu.qqrobotsign.aspect;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hongchu.qqrobotsign.annotation.LogRecord;
import com.hongchu.qqrobotsign.context.BaseContext;
import com.hongchu.qqrobotsign.mapper.UserMapper;
import com.hongchu.qqrobotsign.pojo.DTO.BindPasswordDTO;
import com.hongchu.qqrobotsign.pojo.DTO.QrCreateDTO;
import com.hongchu.qqrobotsign.pojo.DTO.SmsVerifyDTO;
import com.hongchu.qqrobotsign.pojo.DTO.UserDTO;
import com.hongchu.qqrobotsign.pojo.VO.QrPollVO;
import com.hongchu.qqrobotsign.pojo.VO.UserLoginVO;
import com.hongchu.qqrobotsign.pojo.entity.User;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.service.IOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int MAX_DETAIL_LENGTH = 1200;
    private static final Set<String> SAFE_REQUEST_PARAMETERS = Set.of("id", "signId", "schoolId");

    private final IOperationLogService operationLogService;
    private final UserMapper userMapper;

    @Around("@annotation(com.hongchu.qqrobotsign.annotation.LogRecord)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        LogRecord annotation = signature.getMethod().getAnnotation(LogRecord.class);
        HttpServletRequest request = currentRequest();
        AuditIdentity identity = extractIdentity(joinPoint, signature);
        User actor = findUserById(currentUserId());

        Object returnValue = null;
        String result = "SUCCESS";
        String resultDetail = null;
        boolean skipLog = false;

        try {
            returnValue = joinPoint.proceed();
            enrichIdentityFromReturn(identity, returnValue);
            skipLog = isWaitingQrPoll(returnValue);
            result = classifyResult(returnValue);
            resultDetail = extractResultDetail(returnValue);
            return returnValue;
        } catch (Throwable throwable) {
            result = "FAIL";
            resultDetail = safeText(throwable.getMessage(), 500);
            throw throwable;
        } finally {
            if (!skipLog) {
                enrichIdentityFromDatabase(identity);
                if (actor == null && identity.userId != null && identity.userId.equals(currentUserId())) {
                    actor = findUserById(identity.userId);
                }

                long duration = System.currentTimeMillis() - start;
                String requestUri = request != null ? request.getRequestURI() : null;
                String actionMethod = resolveActionMethod(requestUri, identity.role);
                String operator = resolveOperator(actor, identity);
                String detail = buildDetail(annotation.value(), identity, actionMethod,
                        request, joinPoint, signature, resultDetail);

                try {
                    operationLogService.saveDetailed(
                            "API",
                            annotation.value(),
                            detail,
                            result,
                            operator,
                            identity.userId,
                            identity.username,
                            identity.userName,
                            actionMethod,
                            getClientIp(request),
                            requestUri,
                            duration
                    );
                } catch (Exception logException) {
                    log.error("记录操作日志失败", logException);
                }
            }
        }
    }

    private AuditIdentity extractIdentity(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        AuditIdentity identity = new AuditIdentity();
        Object[] args = joinPoint.getArgs();

        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof BindPasswordDTO dto) {
                    identity.username = dto.getUsername();
                } else if (arg instanceof SmsVerifyDTO dto) {
                    identity.username = dto.getUsername();
                } else if (arg instanceof QrCreateDTO dto) {
                    identity.username = dto.getUsername();
                } else if (arg instanceof UserDTO dto && dto.getUsername() != null) {
                    identity.username = dto.getUsername();
                }
            }
        }

        Parameter[] parameters = signature.getMethod().getParameters();
        if (args != null && parameters.length == args.length) {
            for (int i = 0; i < parameters.length; i++) {
                PathVariable pathVariable = parameters[i].getAnnotation(PathVariable.class);
                if (pathVariable == null || !(args[i] instanceof String value)) {
                    continue;
                }
                String name = bindingName(pathVariable.value(), pathVariable.name(), parameters[i]);
                if ("username".equals(name)) {
                    identity.username = value;
                }
            }
        }

        if (identity.username == null) {
            identity.userId = currentUserId();
        }
        enrichIdentityFromDatabase(identity);
        return identity;
    }

    private void enrichIdentityFromReturn(AuditIdentity identity, Object returnValue) {
        if (!(returnValue instanceof Result<?> response)) {
            return;
        }
        Object data = response.getData();
        if (data instanceof UserLoginVO loginVO) {
            applyLoginIdentity(identity, loginVO);
        } else if (data instanceof QrPollVO pollVO && pollVO.getLoginVO() != null) {
            applyLoginIdentity(identity, pollVO.getLoginVO());
        }
    }

    private void applyLoginIdentity(AuditIdentity identity, UserLoginVO loginVO) {
        identity.userId = loginVO.getId();
        identity.username = loginVO.getUsername();
        identity.userName = loginVO.getName();
        identity.role = loginVO.getRole();
    }

    private void enrichIdentityFromDatabase(AuditIdentity identity) {
        User user = identity.userId != null
                ? findUserById(identity.userId)
                : findUserByUsername(identity.username);
        if (user == null) {
            return;
        }
        identity.userId = user.getId();
        identity.username = user.getUsername();
        identity.userName = user.getName();
        identity.role = user.getRole();
    }

    private User findUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return userMapper.selectById(userId);
        } catch (Exception exception) {
            log.warn("操作日志补充用户信息失败-userId: {}", userId);
            return null;
        }
    }

    private User findUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        try {
            return userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, username)
                    .last("LIMIT 1"));
        } catch (Exception exception) {
            log.warn("操作日志补充用户信息失败-username: {}", username);
            return null;
        }
    }

    private String classifyResult(Object returnValue) {
        if (returnValue instanceof Result<?> response) {
            if (response.getCode() != null && response.getCode() != 200) {
                return "FAIL";
            }
            if (response.getData() instanceof QrPollVO pollVO
                    && "EXPIRED".equalsIgnoreCase(pollVO.getStatus())) {
                return "FAIL";
            }
            return "SUCCESS";
        }
        if (returnValue instanceof String text) {
            boolean hasFailure = containsAny(text, "失败", "异常", "错误", "❌", "⚠");
            boolean hasSuccess = containsAny(text, "成功", "完成", "无需签到", "✅", "✔");
            if (hasFailure && hasSuccess) {
                return "PARTIAL";
            }
            if (hasFailure) {
                return "FAIL";
            }
        }
        return "SUCCESS";
    }

    private String extractResultDetail(Object returnValue) {
        if (returnValue == null) {
            return "操作完成";
        }
        if (returnValue instanceof Result<?> response) {
            Object data = response.getData();
            if (data instanceof UserLoginVO loginVO) {
                return "登录成功 | 角色: " + safeValue(loginVO.getRole());
            }
            if (data instanceof QrPollVO pollVO) {
                if ("SUCCESS".equalsIgnoreCase(pollVO.getStatus())) {
                    String role = pollVO.getLoginVO() != null ? pollVO.getLoginVO().getRole() : null;
                    return "扫码登录成功 | 角色: " + safeValue(role);
                }
                return "扫码状态: " + safeValue(pollVO.getStatus());
            }
            if (data instanceof String text && !text.isBlank()) {
                return safeText(text, 600);
            }
            if (data instanceof Map<?, ?> map) {
                return "返回 " + map.size() + " 项数据";
            }
            if (data instanceof Collection<?> collection) {
                return "返回 " + collection.size() + " 条数据";
            }
            return safeText(response.getMessage(), 500);
        }
        if (returnValue instanceof String text) {
            return safeText(text, 800);
        }
        return "操作完成";
    }

    private String buildDetail(String operation, AuditIdentity identity, String actionMethod,
                               HttpServletRequest request, ProceedingJoinPoint joinPoint,
                               MethodSignature signature, String resultDetail) {
        StringBuilder detail = new StringBuilder();
        appendPart(detail, "操作", operation);
        appendPart(detail, "方式", actionMethod);
        appendPart(detail, "学号", identity.username);
        appendPart(detail, "姓名", identity.userName);
        appendPart(detail, "用户ID", identity.userId);
        appendPart(detail, "角色", identity.role);
        if (request != null) {
            appendPart(detail, "请求", request.getMethod() + " " + request.getRequestURI());
        }
        appendSafeRequestParameters(detail, joinPoint, signature);
        appendPart(detail, "结果详情", resultDetail);
        return safeText(detail.toString(), MAX_DETAIL_LENGTH);
    }

    private void appendSafeRequestParameters(StringBuilder detail, ProceedingJoinPoint joinPoint,
                                             MethodSignature signature) {
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = signature.getMethod().getParameters();
        if (args == null || parameters.length != args.length) {
            return;
        }
        for (int i = 0; i < parameters.length; i++) {
            String name = null;
            PathVariable pathVariable = parameters[i].getAnnotation(PathVariable.class);
            RequestParam requestParam = parameters[i].getAnnotation(RequestParam.class);
            if (pathVariable != null) {
                name = bindingName(pathVariable.value(), pathVariable.name(), parameters[i]);
            } else if (requestParam != null) {
                name = bindingName(requestParam.value(), requestParam.name(), parameters[i]);
            }
            if (name != null && SAFE_REQUEST_PARAMETERS.contains(name) && args[i] != null) {
                appendPart(detail, name, args[i]);
            }
        }
    }

    private String resolveActionMethod(String requestUri, String role) {
        if (requestUri == null) {
            return null;
        }
        if (requestUri.endsWith("/user/xsy-login")) {
            return "ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "PASSWORD";
        }
        if (requestUri.endsWith("/user/bind/password")) {
            return "PASSWORD";
        }
        if (requestUri.endsWith("/user/sms/login")) {
            return "SMS";
        }
        if (requestUri.endsWith("/user/qr/poll")) {
            return "QR";
        }
        if (requestUri.contains("/sign/all-admin/")) {
            return "ADMIN";
        }
        if (requestUri.contains("/sign/")) {
            return "MANUAL";
        }
        return null;
    }

    private boolean isWaitingQrPoll(Object returnValue) {
        return returnValue instanceof Result<?> response
                && response.getData() instanceof QrPollVO pollVO
                && "WAITING".equalsIgnoreCase(pollVO.getStatus());
    }

    private String resolveOperator(User actor, AuditIdentity identity) {
        if (actor != null && actor.getUsername() != null) {
            return actor.getUsername();
        }
        if (identity.username != null) {
            return identity.username;
        }
        return "ANONYMOUS";
    }

    private Long currentUserId() {
        try {
            return BaseContext.getCurrentId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (isMissingIp(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isMissingIp(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(',')).trim();
        }
        return ip;
    }

    private boolean isMissingIp(String ip) {
        return ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip);
    }

    private String bindingName(String value, String name, Parameter parameter) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        if (name != null && !name.isBlank()) {
            return name;
        }
        return parameter.getName();
    }

    private void appendPart(StringBuilder builder, String label, Object value) {
        if (value == null || value.toString().isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(" | ");
        }
        builder.append(label).append(": ").append(value);
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    private String safeText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replace('\r', ' ').trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength) + "...";
    }

    private static final class AuditIdentity {
        private Long userId;
        private String username;
        private String userName;
        private String role;
    }
}
