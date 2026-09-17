package com.hongchu.qqrobotsign.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类（Spring Boot 3兼容版）
 */
public class JwtUtil {

    /**
     * 从密钥字符串生成SecretKey
     */
    private static SecretKey getSecretKey(String secretKey) {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT
     * @param secretKey jwt秘钥
     * @param ttlMillis jwt过期时间(毫秒)
     * @param claims 设置的信息
     * @return jwt token
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        try {
            SecretKey key = getSecretKey(secretKey);

            // 过期时间
            long expMillis = System.currentTimeMillis() + ttlMillis;
            Date exp = new Date(expMillis);

            return Jwts.builder()
                    .setClaims(claims)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .setExpiration(exp)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("生成JWT失败", e);
        }
    }

    /**
     * Token解密
     * @param secretKey jwt秘钥
     * @param token 加密后的token
     * @return 解密后的信息
     */
    public static Claims parseJWT(String secretKey, String token) {
        try {
            SecretKey key = getSecretKey(secretKey);

            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("JWT已过期", e);
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException("不支持的JWT格式", e);
        } catch (MalformedJwtException e) {
            throw new RuntimeException("JWT格式错误", e);
        } catch (SecurityException e) {
            throw new RuntimeException("JWT签名验证失败", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("JWT参数错误", e);
        }
    }

    /**
     * 验证token是否有效
     * @param secretKey jwt秘钥
     * @param token 待验证的token
     * @return 是否有效
     */
    public static boolean validateToken(String secretKey, String token) {
        try {
            parseJWT(secretKey, token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取token中的用户ID
     * @param secretKey jwt秘钥
     * @param token jwt token
     * @return 用户ID
     */
    public static Long getUserIdFromToken(String secretKey, String token) {
        Claims claims = parseJWT(secretKey, token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        } else if (userId instanceof String) {
            try {
                return Long.parseLong((String) userId);
            } catch (NumberFormatException e) {
                throw new RuntimeException("用户ID格式错误");
            }
        }
        throw new RuntimeException("用户ID不存在或格式错误");
    }

    /**
     * 获取token中的用户名
     * @param secretKey jwt秘钥
     * @param token jwt token
     * @return 用户名
     */
    public static String getUsernameFromToken(String secretKey, String token) {
        Claims claims = parseJWT(secretKey, token);
        Object username = claims.get("username");
        return username != null ? username.toString() : null;
    }

    /**
     * 刷新token（创建新的相同claims的token）
     * @param secretKey jwt秘钥
     * @param oldToken 旧token
     * @param ttlMillis 新token有效期
     * @return 新token
     */
    public static String refreshToken(String secretKey, String oldToken, long ttlMillis) {
        try {
            SecretKey key = getSecretKey(secretKey);
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(oldToken)
                    .getBody();

            return createJWT(secretKey, ttlMillis, claims);
        } catch (Exception e) {
            throw new RuntimeException("刷新token失败", e);
        }
    }
}