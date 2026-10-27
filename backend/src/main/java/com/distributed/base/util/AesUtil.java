package com.distributed.base.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-128 加解密工具类（用于手机号等隐私数据加密存储）
 */
@Slf4j
@Component
public class AesUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    private final SecretKeySpec secretKey;

    public AesUtil(@Value("${security.aes.key:MealBuddyAesKey!}") String key) {
        byte[] keyBytes = new byte[16];
        byte[] inputBytes = key.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(inputBytes, 0, keyBytes, 0, Math.min(inputBytes.length, 16));
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * 加密明文字符串并返回 Base64 密文
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("[AesUtil] 加密异常: {}", e.getMessage(), e);
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    /**
     * 解密 Base64 密文字符串并返回明文
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[AesUtil] 解密异常: {}", e.getMessage(), e);
            throw new RuntimeException("AES 解密失败", e);
        }
    }

    /**
     * 手机号脱敏：保留前3后4，中间4位星号（138****1234）
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }
}
