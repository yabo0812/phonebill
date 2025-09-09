package com.phonebill.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 제공자
 * JWT 토큰의 생성, 검증, 파싱을 담당
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long tokenValidityInMilliseconds;

    public JwtTokenProvider(@Value("${jwt.secret:}") String secret,
                           @Value("${jwt.access-token-validity:3600}") long tokenValidityInSeconds) {
        if (StringUtils.hasText(secret)) {
            this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        } else {
            // 개발용 기본 시크릿 키 (32바이트 이상)
            this.secretKey = Keys.hmacShaKeyFor("phonebill-default-secret-key-for-development-only".getBytes(StandardCharsets.UTF_8));
            log.warn("JWT secret key not provided, using default development key");
        }
        this.tokenValidityInMilliseconds = tokenValidityInSeconds * 1000;
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * JWT 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.debug("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("JWT token compact of handler are invalid: {}", e.getMessage());
        }
        return false;
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    public String getUserId(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.getSubject();
    }

    /**
     * JWT 토큰에서 사용자명 추출
     */
    public String getUsername(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.get("username", String.class);
    }

    /**
     * JWT 토큰에서 권한 정보 추출
     */
    public String getAuthority(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.get("authority", String.class);
    }

    /**
     * JWT 토큰에서 고객 ID 추출
     */
    public String getCustomerId(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.get("customerId", String.class);
    }

    /**
     * JWT 토큰에서 회선번호 추출
     */
    public String getLineNumber(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.get("lineNumber", String.class);
    }

    /**
     * 토큰 만료 시간 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 토큰에서 만료 시간 추출
     */
    public Date getExpirationDate(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.getExpiration();
    }
}