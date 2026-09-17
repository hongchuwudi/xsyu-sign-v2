package com.hongchu.qqrobotsign.intercepter;

import com.hongchu.qqrobotsign.context.BaseContext;
import com.hongchu.qqrobotsign.properties.JwtProperties;
import com.hongchu.qqrobotsign.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtProperties jwtProperties;

    public JwtInterceptor(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取token
        String token = request.getHeader(jwtProperties.getTokenName());
        if (token == null) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"msg\":\"Token required\"}");
            return false;
        }
        // 移除Bearer前缀
        if (token.startsWith("Bearer ")) token = token.substring(7);
        try {
            // 解析token
            Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
            // 获取用户ID并存入ThreadLocal
            Object userId = claims.get("userId");
            if (userId != null) BaseContext.setCurrentId(Long.parseLong(userId.toString()));
            // 可选：将完整claims存入request
            request.setAttribute("userClaims", claims);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"msg\":\"Invalid token\"}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.clear(); // 清理ThreadLocal，防止内存泄漏
    }
}