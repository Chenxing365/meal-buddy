package com.distributed.base.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("M1阶段-AES手机号加解密与脱敏测试")
class AesUtilTest {

    private AesUtil aesUtil;

    @BeforeEach
    void setUp() {
        aesUtil = new AesUtil("MealBuddyAesKey!");
    }

    @Test
    @DisplayName("测试手机号加密为非明文密文，且解密还原一致")
    void testEncryptAndDecrypt() {
        String phone = "13812345678";
        String encrypted = aesUtil.encrypt(phone);

        assertNotNull(encrypted);
        assertNotEquals(phone, encrypted);

        String decrypted = aesUtil.decrypt(encrypted);
        assertEquals(phone, decrypted);
    }

    @Test
    @DisplayName("测试手机号掩码脱敏（保留前3后4，中间4位星号）")
    void testMaskPhone() {
        String phone = "13812345678";
        String masked = AesUtil.maskPhone(phone);
        assertEquals("138****5678", masked);
    }
}
