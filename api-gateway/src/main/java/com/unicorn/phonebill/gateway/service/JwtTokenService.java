package com.unicorn.phonebill.gateway.service;

import com.unicorn.phonebill.gateway.dto.TokenValidationResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * JWT 토큰 검증 서비스
 * 
 * JWT 토큰의 유효성을 검증합니다.
 * 
 * 주요 기능:
 * - JWT 토큰 파싱 및 서명 검증
 * - 토큰 만료 검사
 * - 사용자 정보 추출
 * - 토큰 갱신 필요 여부 판단
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
@Service
public class JwtTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);
    
    private final SecretKey secretKey;
    private final long accessTokenValidityInSeconds;
    private final long refreshTokenValidityInSeconds;
    
    public JwtTokenService(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.access-token-validity-in-seconds:1800}") long accessTokenValidityInSeconds,
            @Value("${app.jwt.refresh-token-validity-in-seconds:86400}") long refreshTokenValidityInSeconds) {
        
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
        this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
        
        logger.info("JWT Token Service initialized - Access token validity: {}s, Refresh token validity: {}s", 
                   accessTokenValidityInSeconds, refreshTokenValidityInSeconds);
    }

    /**
     * JWT 토큰 검증 (비동기)
     * 
     * @param token JWT 토큰
     * @return TokenValidationResult
     */
    public Mono<TokenValidationResult> validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Mono.just(TokenValidationResult.invalid("토큰이 비어있습니다"));
        }
        
        try {
            // JWT 토큰 파싱 및 서명 검증
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            // 기본 정보 추출
            String userId = claims.getSubject();
            String userRole = claims.get("role", String.class);
            Instant expiresAt = claims.getExpiration().toInstant();
            String tokenId = claims.getId(); // jti claim
            
            if (userId == null || userId.trim().isEmpty()) {
                return Mono.just(TokenValidationResult.invalid("사용자 정보가 없습니다"));
            }
            
            // 토큰 만료 검사
            if (expiresAt.isBefore(Instant.now())) {
                return Mono.just(TokenValidationResult.invalid("토큰이 만료되었습니다"));
            }
            
            // 토큰 갱신 필요 여부 판단 (만료 10분 전)
            boolean needsRefresh = expiresAt.isBefore(
                    Instant.now().plus(Duration.ofMinutes(10))
            );
            
            return Mono.just(TokenValidationResult.valid(userId, userRole, expiresAt, needsRefresh));
                    
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token expired: {}", e.getMessage());
            return Mono.just(TokenValidationResult.invalid("토큰이 만료되었습니다"));
            
        } catch (UnsupportedJwtException e) {
            logger.debug("Unsupported JWT token: {}", e.getMessage());
            return Mono.just(TokenValidationResult.invalid("지원하지 않는 토큰 형식입니다"));
            
        } catch (MalformedJwtException e) {
            logger.debug("Malformed JWT token: {}", e.getMessage());
            return Mono.just(TokenValidationResult.invalid("잘못된 토큰 형식입니다"));
            
        } catch (SignatureException e) {
            logger.debug("Invalid JWT signature: {}", e.getMessage());
            return Mono.just(TokenValidationResult.invalid("토큰 서명이 유효하지 않습니다"));
            
        } catch (IllegalArgumentException e) {
            logger.debug("Empty JWT token: {}", e.getMessage());
            return Mono.just(TokenValidationResult.invalid("토큰이 비어있습니다"));
            
        } catch (Exception e) {
            logger.error("JWT token validation error: {}", e.getMessage(), e);
            return Mono.just(TokenValidationResult.invalid("토큰 검증 중 오류가 발생했습니다"));
        }
    }

    // Redis 블랙리스트 기능은 API Gateway에서 제거됨
    // 필요한 경우 각 마이크로서비스에서 직접 처리

    /**
     * 토큰에서 사용자 ID 추출 (검증 없이)
     * 
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public String extractUserIdWithoutValidation(String token) {
        try {
            // 서명 검증 없이 클레임만 추출
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getSubject();
        } catch (Exception e) {
            logger.debug("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰 만료 시간까지 남은 시간 계산
     * 
     * @param token JWT 토큰
     * @return 남은 시간 (초)
     */
    public Long getTokenRemainingTime(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            Instant expiresAt = claims.getExpiration().toInstant();
            Duration remaining = Duration.between(Instant.now(), expiresAt);
            
            return remaining.isNegative() ? 0L : remaining.getSeconds();
        } catch (Exception e) {
            logger.debug("Failed to get token remaining time: {}", e.getMessage());
            return 0L;
        }
    }
}