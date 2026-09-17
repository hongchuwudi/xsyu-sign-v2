package com.hongchu.qqrobotsign.service;

import com.hongchu.qqrobotsign.pojo.entity.CasSmsState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * CAS 短信会话服务。
 * <p>
 * 基于 Redis 管理短信登录过程中的会话状态（{@link CasSmsState}），
 * 用于在短信验证码登录流程中跨请求保存和读取状态信息。
 * <p>
 * 会话数据以 {@code cas:sms:{sessionId}} 为 key 存储在 Redis 中，
 * 默认有效期为 300 秒（5 分钟），超过有效期后自动过期失效。
 *
 * @author hongchu
 */
@Service
@Slf4j
public class CasSmsSessionService {

    // Redis key 前缀，完整 key 形如：cas:sms:{sessionId}
    private static final String KEY_PREFIX = "cas:sms:";

    // 会话过期时间，单位：秒（默认 5 分钟）
    private static final long TTL_SECONDS = 300;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 保存短信登录状态，生成并返回新的会话 ID
    public String saveState(CasSmsState state) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, state, TTL_SECONDS, TimeUnit.SECONDS);
        log.info("短信登录状态已保存 - sessionId: {}, phone: {}", sessionId, state.getPhone());
        return sessionId;
    }

    // 根据会话 ID 获取短信登录状态，不存在或已过期返回 null
    public CasSmsState getState(String sessionId) {
        return (CasSmsState) redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
    }

    // 刷新指定会话的过期时间，重置为 TTL_SECONDS 秒
    public void refreshTtl(String sessionId) {
        redisTemplate.expire(KEY_PREFIX + sessionId, TTL_SECONDS, TimeUnit.SECONDS);
    }

    // 删除指定会话的状态，通常在短信流程结束后调用
    public void deleteState(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }
}
