package com.hongchu.qqrobotsign.utils;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA非对称加密工具类
 * 用于密码的加密传输
 */
public class RSAUtils {

    /**
     * 从Base64字符串加载公钥
     */
    public static PublicKey loadPublicKey(String base64PublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    /**
     * 从Base64字符串加载私钥
     */
    public static PrivateKey loadPrivateKey(String base64PrivateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    /**
     * 使用公钥加密
     */
    public static String encrypt(String plainText, String base64PublicKey) throws Exception {
        PublicKey publicKey = loadPublicKey(base64PublicKey);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * 使用私钥解密
     */
    public static String decrypt(String encryptedText, String base64PrivateKey) throws Exception {
        PrivateKey privateKey = loadPrivateKey(base64PrivateKey);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes, "UTF-8");
    }

    /**
     * 生成新的RSA密钥对（用于初始化）
     */
    public static void generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        System.out.println("===== RSA Public Key (for frontend) =====");
        System.out.println(publicKey);
        System.out.println();
        System.out.println("===== RSA Private Key (for backend) =====");
        System.out.println(privateKey);
    }

    public static void main(String[] args) throws Exception {
        generateKeyPair();
    }
}
