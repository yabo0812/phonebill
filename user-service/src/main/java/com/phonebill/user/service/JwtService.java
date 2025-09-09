package com.phonebill.user.service;

import com.phonebill.user.config.JwtConfig;
import com.phonebill.user.entity.AuthUserEntity;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 토큰 관리 서비스
 * JWT 토큰 생성, 검증, 파싱 등을 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    
    private final JwtConfig jwtConfig;
    
    /**
     * Access Token 생성
     * @param user 사용자 정보
     * @return Access Token
     */
    public String generateAccessToken(AuthUserEntity user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("customerId", user.getCustomerId());
        claims.put("lineNumber", user.getLineNumber());
        claims.put("type", "ACCESS");
        
        return createToken(claims, user.getUserId(), jwtConfig.getAccessTokenValidity());
    }
    
    /**
     * Refresh Token 생성
     * @param user 사용자 정보
     * @return Refresh Token
     */
    public String generateRefreshToken(AuthUserEntity user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("type", "REFRESH");
        
        return createToken(claims, user.getUserId(), jwtConfig.getRefreshTokenValidity());
    }
    
    /**
     * JWT 토큰 생성
     * @param claims 클레임 정보
     * @param subject 주체 (사용자 ID)
     * @param validity 유효시간 (milliseconds)
     * @return JWT 토큰
     */
    private String createToken(Map<String, Object> claims, String subject, long validity) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(jwtConfig.getIssuer())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * 토큰에서 사용자 ID 추출
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }
    
    /**
     * 토큰에서 고객 ID 추출
     * @param token JWT 토큰
     * @return 고객 ID
     */
    public String getCustomerIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (String) claims.get("customerId") : null;
    }
    
    /**
     * 토큰에서 회선번호 추출
     * @param token JWT 토큰
     * @return 회선번호
     */
    public String getLineNumberFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (String) claims.get("lineNumber") : null;
    }
    
    /**
     * 토큰에서 만료일 추출
     * @param token JWT 토큰
     * @return 만료일
     */
    public LocalDateTime getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null && claims.getExpiration() != null) {
            return LocalDateTime.ofInstant(
                claims.getExpiration().toInstant(), 
                ZoneId.systemDefault()
            );
        }
        return null;
    }
    
    /**
     * 토큰 유효성 검증 (JWT 자체만 검증, 블랙리스트는 AuthService에서 확인)
     * @param token JWT 토큰
     * @return 유효성 여부
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims != null && !isTokenExpired(claims);
        } catch (Exception e) {
            log.warn("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Access Token 여부 확인
     * @param token JWT 토큰
     * @return Access Token 여부
     */
    public boolean isAccessToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null && "ACCESS".equals(claims.get("type"));
    }
    
    /**
     * Refresh Token 여부 확인
     * @param token JWT 토큰
     * @return Refresh Token 여부
     */
    public boolean isRefreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null && "REFRESH".equals(claims.get("type"));
    }
    
    /**
     * 토큰에서 클레임 정보 추출
     * @param token JWT 토큰
     * @return 클레임 정보
     */
    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT 토큰 만료: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("잘못된 형식의 JWT 토큰: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.error("JWT 서명 검증 실패: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 비어있음: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("JWT 토큰 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 토큰 만료 여부 확인
     * @param claims 클레임 정보
     * @return 만료 여부
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }
    
    /**
     * JWT 서명 키 생성
     * @return 서명 키
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 토큰 타입 확인
     * @param token JWT 토큰
     * @return 토큰 타입 ("ACCESS", "REFRESH", null)
     */
    public String getTokenType(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (String) claims.get("type") : null;
    }
    
    /**
     * 토큰 발급자 확인
     * @param token JWT 토큰
     * @return 발급자
     */
    public String getIssuerFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getIssuer() : null;
    }
    
    /**
     * 토큰 발급 시간 확인
     * @param token JWT 토큰
     * @return 발급 시간
     */
    public LocalDateTime getIssuedAtFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null && claims.getIssuedAt() != null) {
            return LocalDateTime.ofInstant(
                claims.getIssuedAt().toInstant(), 
                ZoneId.systemDefault()
            );
        }
        return null;
    }
}