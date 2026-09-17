package com.hongchu.qqrobotsign.service;

import com.hongchu.qqrobotsign.pojo.entity.CasCaptchaState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CaptchaService {
    private static final String KEY_PREFIX = "cas:captcha:";
    private static final long TTL_SECONDS = 300;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public String saveState(CasCaptchaState state) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String key = KEY_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, state, TTL_SECONDS, TimeUnit.SECONDS);
        log.info("验证码状态已保存 - sessionId: {}, username: {}", sessionId, state.getUsername());
        return sessionId;
    }

    public CasCaptchaState getState(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        CasCaptchaState state = (CasCaptchaState) redisTemplate.opsForValue().get(key);
        if (state == null) {
            log.warn("验证码状态未找到或已过期 - sessionId: {}", sessionId);
        }
        return state;
    }

    public void deleteState(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        redisTemplate.delete(key);
    }
}
