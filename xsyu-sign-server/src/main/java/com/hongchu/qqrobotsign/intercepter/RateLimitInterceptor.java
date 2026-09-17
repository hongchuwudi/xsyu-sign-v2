package com.hongchu.qqrobotsign.intercepter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// 限流 - 使用Guava RateLimiter
@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final com.google.common.util.concurrent.RateLimiter rateLimiter =
            com.google.common.util.concurrent.RateLimiter.create(66.0); // 每秒66个请求

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 使用acquire()而不是tryAcquire()
        double waitTime = rateLimiter.acquire();
        if (waitTime == 0) {
            // 立即获得许可
            return true;
        } else {
            log.info("请求被限流，等待时间: {}秒", waitTime);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 429, \"message\": \"请求过于频繁，请稍后再试\"}");
            return false;
        }
    }
}