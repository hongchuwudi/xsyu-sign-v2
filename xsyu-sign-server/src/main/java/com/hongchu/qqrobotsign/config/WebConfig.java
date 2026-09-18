package com.hongchu.qqrobotsign.config;

import com.hongchu.qqrobotsign.intercepter.AdminAuthInterceptor;
import com.hongchu.qqrobotsign.intercepter.JwtInterceptor;
import com.hongchu.qqrobotsign.intercepter.RateLimitInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Autowired private RateLimitInterceptor rateLimitInterceptor;
    @Autowired private JwtInterceptor jwtInterceptor;
    @Autowired private AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("🚀 注册限流拦截器...");
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/favicon.ico",
                        // 静态资源路径
                        "/static/**",
                        "/public/**",
                        "/resources/**",
                        // 前端页面
                        "/index.html",
                        "/",
                        // 前端资源
                        "/assets/**",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/screenshots/**",
                        "/fonts/**"
                )
                .order(1);
        log.info("✅ 限流拦截器注册成功");

        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/index.html",
                        "/",
                        "/test/**",
                        "/user/xsy-login",
                        "/user/test-login",
                        "/user/public-key",
                        "/user/sms/send",
                        "/user/sms/login",
                        "/user/qr/create",
                        "/user/qr/poll",
                        "/error",
                        "/favicon.ico",
                        // 静态资源路径
                        "/static/**",
                        "/public/**",
                        "/resources/**",
                        // 前端资源
                        "/assets/**",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/screenshots/**",
                        "/fonts/**",
                        // 开放API文档
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**"
                )
                .order(2);
        log.info("✅ JWT拦截器注册成功");

        // 注册管理员权限拦截器
        log.info("🚀 注册管理员权限拦截器...");
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns(
                        "/admin/**",           // 管理员用户管理
                        "/sign/all-admin/**",  // 管理员为用户签到
                        "/sign/all-all"        // 一键为所有用户签到
                )
                .order(3);
        log.info("✅ 管理员权限拦截器注册成功");
    }
}
