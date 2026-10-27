package com.distributed.base.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("M1阶段-JJWT生成与校验测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
                "MealBuddySecretKeyForJwtAuthenticationTokenSigning20261027",
                7200000L,
                604800000L
        );
    }

    @Test
    @DisplayName("测试生成 Access Token 与 Refresh Token 并成功解析 Claims")
    void testGenerateAndParseToken() {
        String userId = "user_123456";
        String username = "jens_test";
        Integer role = 0;

        String accessToken = jwtUtil.generateAccessToken(userId, username, role);
        assertNotNull(accessToken);
        assertTrue(jwtUtil.validateToken(accessToken));

        Claims claims = jwtUtil.parseToken(accessToken);
        assertEquals(userId, claims.getSubject());
        assertEquals(username, claims.get("username", String.class));
        assertEquals(role, claims.get("role", Integer.class));
        assertEquals("access", claims.get("type", String.class));

        String refreshToken = jwtUtil.generateRefreshToken(userId, username, role);
        assertNotNull(refreshToken);
        assertTrue(jwtUtil.validateToken(refreshToken));

        Claims refreshClaims = jwtUtil.parseToken(refreshToken);
        assertEquals("refresh", refreshClaims.get("type", String.class));
    }
}
