package com.hongchu.qqrobotsign;

import com.hongchu.qqrobotsign.utils.CryptoUtils;

public class TestCryptoUtils {
    public static void main(String[] args) {
        System.out.println("=== 强加密工具类测试 ===\n");

        // 测试1: 基本加解密
        testBasicEncryption();

//        // 测试2: 密码验证
//        testPasswordVerification();
//
//        // 测试3: 生成随机密码
//        testRandomPasswordGeneration();
//
//        // 测试4: 密码哈希
//        testPasswordHashing();
//
//        // 测试5: 边界测试
//        testEdgeCases();
//
//        // 测试6: 性能测试
//        testPerformance();
    }

    /**
     * 测试基本加解密功能
     */
    private static void testBasicEncryption() {
        System.out.println("1. 基本加解密测试:");

        String originalText = "xxxx";
        System.out.println("原始文本: " + originalText);

        // 加密
        String encryptedText = CryptoUtils.encrypt(originalText);
        System.out.println("加密结果: " + encryptedText);
        System.out.println("加密格式: 盐值(" + encryptedText.split(":")[0].length() + "字符):初始向量(" +
                encryptedText.split(":")[1].length() + "字符):密文(" +
                encryptedText.split(":")[2].length() + "字符)");

        // 解密
        String decryptedText = CryptoUtils.decrypt(encryptedText);
        System.out.println("解密结果: " + decryptedText);

        // 验证
        boolean success = originalText.equals(decryptedText);
        System.out.println("加解密验证: " + (success ? "✅ 成功" : "❌ 失败"));
        System.out.println();
    }

    /**
     * 测试密码验证功能
     */
    private static void testPasswordVerification() {
        System.out.println("2. 密码验证测试:");

        String password = "mySecretPassword123";
        System.out.println("原始密码: " + password);

        // 加密密码
        String encryptedPassword = CryptoUtils.encrypt(password);
        System.out.println("加密密码: " + encryptedPassword.substring(0, 30) + "...");

        // 正确密码验证
        boolean correctVerify = CryptoUtils.verifyPassword(password, encryptedPassword);
        System.out.println("正确密码验证: " + (correctVerify ? "✅ 通过" : "❌ 失败"));

        // 错误密码验证
        boolean wrongVerify = CryptoUtils.verifyPassword("wrongPassword", encryptedPassword);
        System.out.println("错误密码验证: " + (!wrongVerify ? "✅ 拒绝" : "❌ 错误通过"));

        System.out.println();
    }

    /**
     * 测试随机密码生成
     */
    private static void testRandomPasswordGeneration() {
        System.out.println("3. 随机密码生成测试:");

        // 生成不同长度的随机密码
        for (int length : new int[]{8, 12, 16, 20}) {
            String randomPassword = CryptoUtils.generateRandomPassword(length);
            System.out.println(length + "位随机密码: " + randomPassword + " (长度: " + randomPassword.length() + ")");
        }

        System.out.println();
    }

    /**
     * 测试密码哈希功能
     */
    private static void testPasswordHashing() {
        System.out.println("4. 密码哈希测试:");

        String password = "userPassword456";
        System.out.println("原始密码: " + password);

        // 生成密码哈希
        String passwordHash = CryptoUtils.generatePasswordHash(password);
        System.out.println("密码哈希: " + passwordHash);
        System.out.println("哈希格式: 迭代次数(" + passwordHash.split(":")[0] + "):盐值(" +
                passwordHash.split(":")[1].length() + "字符):哈希值(" +
                passwordHash.split(":")[2].length() + "字符)");

        // 正确密码验证
        boolean correctHashVerify = CryptoUtils.verifyPasswordHash(password, passwordHash);
        System.out.println("正确密码哈希验证: " + (correctHashVerify ? "✅ 通过" : "❌ 失败"));

        // 错误密码验证
        boolean wrongHashVerify = CryptoUtils.verifyPasswordHash("wrongPassword", passwordHash);
        System.out.println("错误密码哈希验证: " + (!wrongHashVerify ? "✅ 拒绝" : "❌ 错误通过"));

        System.out.println();
    }

    /**
     * 测试边界情况
     */
    private static void testEdgeCases() {
        System.out.println("5. 边界情况测试:");

        // 测试空字符串
        try {
            String emptyEncrypted = CryptoUtils.encrypt("");
            String emptyDecrypted = CryptoUtils.decrypt(emptyEncrypted);
            System.out.println("空字符串测试: " + ("".equals(emptyDecrypted) ? "✅ 成功" : "❌ 失败"));
        } catch (Exception e) {
            System.out.println("空字符串测试: ❌ 异常 - " + e.getMessage());
        }

        // 测试长文本
        try {
            String longText = "A".repeat(1000);
            String longEncrypted = CryptoUtils.encrypt(longText);
            String longDecrypted = CryptoUtils.decrypt(longEncrypted);
            boolean longSuccess = longText.equals(longDecrypted);
            System.out.println("长文本测试(1000字符): " + (longSuccess ? "✅ 成功" : "❌ 失败"));
        } catch (Exception e) {
            System.out.println("长文本测试: ❌ 异常 - " + e.getMessage());
        }

        // 测试特殊字符
        try {
            String specialText = "密码!@#$%^&*()_+中文测试🚀";
            String specialEncrypted = CryptoUtils.encrypt(specialText);
            String specialDecrypted = CryptoUtils.decrypt(specialEncrypted);
            boolean specialSuccess = specialText.equals(specialDecrypted);
            System.out.println("特殊字符测试: " + (specialSuccess ? "✅ 成功" : "❌ 失败"));
        } catch (Exception e) {
            System.out.println("特殊字符测试: ❌ 异常 - " + e.getMessage());
        }

        // 测试无效加密格式
        try {
            CryptoUtils.decrypt("invalid:format");
            System.out.println("无效格式测试: ❌ 应该抛出异常");
        } catch (Exception e) {
            System.out.println("无效格式测试: ✅ 正确抛出异常");
        }

        System.out.println();
    }

    /**
     * 性能测试
     */
    private static void testPerformance() {
        System.out.println("6. 性能测试:");

        int testCount = 100;
        String testText = "performanceTestPassword";

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < testCount; i++) {
            String encrypted = CryptoUtils.encrypt(testText + i);
            String decrypted = CryptoUtils.decrypt(encrypted);
            if (!(testText + i).equals(decrypted)) {
                System.out.println("性能测试: ❌ 第 " + i + " 次加解密失败");
                return;
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double averageTime = (double) totalTime / testCount;

        System.out.println("完成 " + testCount + " 次加解密测试");
        System.out.println("总耗时: " + totalTime + "ms");
        System.out.println("平均每次: " + String.format("%.2f", averageTime) + "ms");
        System.out.println("性能测试: ✅ 全部成功");
        System.out.println();
    }
}