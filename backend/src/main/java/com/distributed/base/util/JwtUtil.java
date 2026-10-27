package com.distributed.base.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类（处理 Access Token 2小时与 Refresh Token 7天）
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(
            @Value("${security.jwt.secret:MealBuddySecretKeyForJwtAuthenticationTokenSigning20261027}") String secret,
            @Value("${security.jwt.access-token-expiration:7200000}") long accessTokenExpiration,
            @Value("${security.jwt.refresh-token-expiration:604800000}") long refreshTokenExpiration) {
        // 保证密钥至少 256 位 (32 字节)
        byte[] keyBytes = new byte[32];
        byte[] inputBytes = secret.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(inputBytes, 0, keyBytes, 0, Math.min(inputBytes.length, 32));
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * 生成 Access Token（默认 2 小时）
     */
    public String generateAccessToken(String userId, String username, Integer role) {
        return buildToken(userId, username, role, accessTokenExpiration, "access");
    }

    /**
     * 生成 Refresh Token（默认 7 天）
     */
    public String generateRefreshToken(String userId, String username, Integer role) {
        return buildToken(userId, username, role, refreshTokenExpiration, "refresh");
    }

    private String buildToken(String userId, String username, Integer role, long expirationMs, String type) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("username", username)
                .claim("role", role)
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验 Token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 判断 Token 是否合法
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.warn("[JwtUtil] Token 校验失败: {}", e.getMessage());
            return false;
        }
    }
}
