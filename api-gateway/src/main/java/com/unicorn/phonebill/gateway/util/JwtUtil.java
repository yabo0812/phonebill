package com.unicorn.phonebill.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 유틸리티 클래스
 * 
 * JWT 토큰 관련 유틸리티 메서드를 제공합니다.
 * 주로 디버깅이나 개발 과정에서 사용되는 헬퍼 메서드들입니다.
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
public class JwtUtil {
    
    private static final String DEFAULT_SECRET = "phonebill-api-gateway-jwt-secret-key-256-bit-minimum-length-required";

    /**
     * JWT 토큰에서 클레임 추출 (검증 없이)
     * 
     * @param token JWT 토큰
     * @param secretKey 비밀키
     * @return Claims
     */
    public static Claims extractClaimsWithoutVerification(String token, String secretKey) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     * 
     * @param token JWT 토큰
     * @param secretKey 비밀키
     * @return 사용자 ID
     */
    public static String extractUserId(String token, String secretKey) {
        Claims claims = extractClaimsWithoutVerification(token, secretKey);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * JWT 토큰에서 사용자 역할 추출
     * 
     * @param token JWT 토큰
     * @param secretKey 비밀키
     * @return 사용자 역할
     */
    public static String extractUserRole(String token, String secretKey) {
        Claims claims = extractClaimsWithoutVerification(token, secretKey);
        return claims != null ? claims.get("role", String.class) : null;
    }

    /**
     * JWT 토큰 만료 시간 확인
     * 
     * @param token JWT 토큰
     * @param secretKey 비밀키
     * @return 만료 시간
     */
    public static Instant extractExpiration(String token, String secretKey) {
        Claims claims = extractClaimsWithoutVerification(token, secretKey);
        if (claims != null && claims.getExpiration() != null) {
            return claims.getExpiration().toInstant();
        }
        return null;
    }

    /**
     * JWT 토큰 만료 여부 확인
     * 
     * @param token JWT 토큰
     * @param secretKey 비밀키
     * @return 만료 여부
     */
    public static boolean isTokenExpired(String token, String secretKey) {
        Instant expiration = extractExpiration(token, secretKey);
        return expiration != null && expiration.isBefore(Instant.now());
    }

    /**
     * 토큰 남은 시간 계산 (초)
     * 
     * @param token JWT 토큰
     * @param secretKey 비밀키
     * @return 남은 시간 (초), 만료된 경우 0
     */
    public static long getTokenRemainingTimeSeconds(String token, String secretKey) {
        Instant expiration = extractExpiration(token, secretKey);
        if (expiration == null) {
            return 0L;
        }
        
        long remainingSeconds = expiration.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0L, remainingSeconds);
    }

    /**
     * Bearer 토큰에서 JWT 부분만 추출
     * 
     * @param bearerToken Bearer 토큰 (Authorization 헤더 값)
     * @return JWT 토큰 부분
     */
    public static String extractJwtFromBearer(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * JWT 토큰의 기본 정보 요약
     * 
     * @param token JWT 토큰
     * @param secretKey 비밀키
     * @return 토큰 정보 문자열
     */
    public static String getTokenSummary(String token, String secretKey) {
        try {
            Claims claims = extractClaimsWithoutVerification(token, secretKey);
            if (claims == null) {
                return "Invalid token";
            }
            
            return String.format(
                "User: %s, Role: %s, Expires: %s, Remaining: %d seconds",
                claims.getSubject(),
                claims.get("role", String.class),
                claims.getExpiration(),
                getTokenRemainingTimeSeconds(token, secretKey)
            );
        } catch (Exception e) {
            return "Token parsing error: " + e.getMessage();
        }
    }

    /**
     * 개발용 임시 토큰 생성 (테스트 목적)
     * 
     * @param userId 사용자 ID
     * @param userRole 사용자 역할
     * @param validitySeconds 유효 시간 (초)
     * @return JWT 토큰
     */
    public static String createDevelopmentToken(String userId, String userRole, long validitySeconds) {
        SecretKey key = Keys.hmacShaKeyFor(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8));
        
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(validitySeconds);
        
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", userRole)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .setId("DEV-" + System.currentTimeMillis())
                .signWith(key)
                .compact();
    }
}