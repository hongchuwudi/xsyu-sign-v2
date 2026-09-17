package com.hongchu.qqrobotsign.intercepter;

import com.hongchu.qqrobotsign.config.props.AdminConfig;
import com.hongchu.qqrobotsign.context.BaseContext;
import com.hongchu.qqrobotsign.properties.JwtProperties;
import com.hongchu.qqrobotsign.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员权限拦截器
 * 用于拦截管理员接口，校验用户是否为管理员
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    private final JwtProperties jwtProperties;
    private final AdminConfig adminConfig;

    public AdminAuthInterceptor(JwtProperties jwtProperties, AdminConfig adminConfig) {
        this.jwtProperties = jwtProperties;
        this.adminConfig = adminConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取token
        String token = request.getHeader(jwtProperties.getTokenName());
        if (token == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Token required\"}");
            return false;
        }

        // 移除Bearer前缀
        if (token.startsWith("Bearer ")) token = token.substring(7);

        try {
            // 解析token
            Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
            // 获取用户名
            String username = claims.get("username", String.class);
            // 校验是否为管理员：优先 JWT role claim，兼容旧 token 用 adminConfig 用户名兜底
            String role = claims.get("role", String.class);
            boolean isAdmin = "ADMIN".equalsIgnoreCase(role)
                    || (role == null && adminConfig.getUsername().equalsIgnoreCase(username));
            if (!isAdmin) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"msg\":\"无权限访问，仅管理员可操作\"}");
                return false;
            }
            // 获取用户ID并存入ThreadLocal
            Object userId = claims.get("userId");
            if (userId != null) BaseContext.setCurrentId(Long.parseLong(userId.toString()));
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Invalid token\"}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.clear();
    }
}
