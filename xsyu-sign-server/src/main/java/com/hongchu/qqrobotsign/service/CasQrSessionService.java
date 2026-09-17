package com.hongchu.qqrobotsign.service;

import com.hongchu.qqrobotsign.pojo.entity.CasQrState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * CAS 扫码会话服务。
 * <p>
 * 基于 Redis 管理扫码绑定过程中的会话状态（{@link CasQrState}），
 * 用于在扫码登录/绑定流程中跨请求保存和读取状态信息。
 * <p>
 * 会话数据以 {@code cas:qr:{sessionId}} 为 key 存储在 Redis 中，
 * 默认有效期为 600 秒（10 分钟），超过有效期后自动过期失效。
 *
 * @author hongchu
 */
@Service
@Slf4j
public class CasQrSessionService {

    // Redis key 前缀，完整 key 形如：cas:qr:{sessionId}
    private static final String KEY_PREFIX = "cas:qr:";

    // 会话过期时间，单位：秒（默认 10 分钟）
    private static final long TTL_SECONDS = 600;

    // Redis 操作模板，用于读写会话状态
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 保存扫码绑定状态，生成并返回新的会话 ID
    public String saveState(CasQrState state) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, state, TTL_SECONDS, TimeUnit.SECONDS);
        log.info("扫码绑定状态已保存 - sessionId: {}", sessionId);
        return sessionId;
    }

    // 根据会话 ID 获取扫码绑定状态，不存在或已过期返回 null
    public CasQrState getState(String sessionId) {
        return (CasQrState) redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
    }

    // 刷新指定会话的过期时间，重置为 TTL_SECONDS 秒
    public void refreshTtl(String sessionId) {
        redisTemplate.expire(KEY_PREFIX + sessionId, TTL_SECONDS, TimeUnit.SECONDS);
    }

    // 删除指定会话的状态，通常在扫码流程结束后调用
    public void deleteState(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }
}