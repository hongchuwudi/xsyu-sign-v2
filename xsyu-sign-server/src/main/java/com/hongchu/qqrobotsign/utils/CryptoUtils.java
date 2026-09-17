package com.hongchu.qqrobotsign.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * 强加密工具类
 * 使用 AES-GCM 加密算法，支持盐值和迭代次数
 */
public class CryptoUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // GCM认证标签长度
    private static final int IV_LENGTH_BYTE = 12;  // GCM初始向量长度
    private static final int SALT_LENGTH_BYTE = 16; // 盐值长度

    // 主密码，可以从配置文件中读取
    private static final String MASTER_PASSWORD = "hc-strong-master-password-2025!";
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;

    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * 加密字符串
     * @param plainText 明文
     * @return 加密后的字符串（格式: 盐值:初始向量:密文）
     */
    public static String encrypt(String plainText) {
        try {
            // 生成随机盐值
            byte[] salt = new byte[SALT_LENGTH_BYTE];
            secureRandom.nextBytes(salt);

            // 生成随机初始向量
            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            // 从主密码和盐值派生密钥
            SecretKey secretKey = deriveKey(MASTER_PASSWORD, salt);

            // 初始化加密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 加密数据
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 组合结果: 盐值:初始向量:密文
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);

            return saltBase64 + ":" + ivBase64 + ":" + encryptedBase64;

        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密字符串
     * @param encryptedText 加密的字符串（格式: 盐值:初始向量:密文）
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedText) {
        try {
            // 解析加密字符串
            String[] parts = encryptedText.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("无效的加密格式");
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encryptedBytes = Base64.getDecoder().decode(parts[2]);

            // 从主密码和盐值派生密钥
            SecretKey secretKey = deriveKey(MASTER_PASSWORD, salt);

            // 初始化解密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 解密数据
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 从密码和盐值派生密钥
     */
    private static SecretKey deriveKey(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATION_COUNT,
                KEY_LENGTH
        );
        SecretKey secretKey = factory.generateSecret(spec);
        return new SecretKeySpec(secretKey.getEncoded(), "AES");
    }

    /**
     * 校验密码
     * @param inputPassword 用户输入的密码
     * @param encryptedPassword 数据库中存储的加密密码
     * @return 校验结果
     */
    public static boolean verifyPassword(String inputPassword, String encryptedPassword) {
        try {
            String decryptedPassword = decrypt(encryptedPassword);
            return inputPassword.equals(decryptedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成安全的随机密码
     * @param length 密码长度
     * @return 随机密码
     */
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 生成密码哈希（用于不可逆存储）
     * @param password 密码
     * @return 哈希值（格式: 迭代次数:盐值:哈希值）
     */
    public static String generatePasswordHash(String password) {
        try {
            // 生成随机盐值
            byte[] salt = new byte[16];
            secureRandom.nextBytes(salt);

            // 使用PBKDF2生成哈希
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 310000, 256);
            byte[] hash = factory.generateSecret(spec).getEncoded();

            // 组合结果: 迭代次数:盐值:哈希值
            return "310000:" +
                    Base64.getEncoder().encodeToString(salt) + ":" +
                    Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new RuntimeException("生成密码哈希失败", e);
        }
    }

    /**
     * 验证密码哈希
     * @param password 待验证的密码
     * @param storedHash 存储的哈希值
     * @return 验证结果
     */
    public static boolean verifyPasswordHash(String password, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 3) {
                return false;
            }

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            byte[] testHash = factory.generateSecret(spec).getEncoded();

            // 使用恒定时间比较防止时序攻击
            return constantTimeEquals(expectedHash, testHash);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 恒定时间比较，防止时序攻击
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}