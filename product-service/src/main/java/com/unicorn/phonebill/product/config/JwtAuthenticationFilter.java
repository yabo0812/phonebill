package com.unicorn.phonebill.product.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * JWT 인증 필터
 * 
 * 주요 기능:
 * - Authorization 헤더에서 JWT 토큰 추출
 * - JWT 토큰 검증 및 파싱
 * - 사용자 인증 정보를 SecurityContext에 설정
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Value("${app.jwt.secret:mySecretKey}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration:86400}")
    private long jwtExpirationInSeconds;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // JWT 토큰 추출
            String jwt = resolveToken(request);
            
            if (StringUtils.hasText(jwt) && validateToken(jwt)) {
                // JWT에서 사용자 정보 추출
                Authentication authentication = getAuthenticationFromToken(jwt);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // 사용자 정보를 헤더에 추가 (다운스트림 서비스에서 활용)
                addUserInfoToHeaders(request, response, jwt);
            }
        } catch (Exception ex) {
            logger.error("JWT 인증 처리 중 오류 발생", ex);
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 JWT 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * JWT 토큰 유효성 검증
     */
    private boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("JWT 토큰이 유효하지 않습니다: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT 토큰이 만료되었습니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT 클레임이 비어있습니다: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("JWT 토큰 검증 중 오류 발생: {}", e.getMessage());
        }
        return false;
    }

    /**
     * JWT 토큰에서 인증 정보 추출
     */
    private Authentication getAuthenticationFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        String userId = claims.getSubject();
        String authorities = claims.get("auth", String.class);
        
        Collection<SimpleGrantedAuthority> grantedAuthorities = 
            StringUtils.hasText(authorities) ? 
            Arrays.stream(authorities.split(","))
                  .map(SimpleGrantedAuthority::new)
                  .collect(Collectors.toList()) : 
            Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
        
        return new UsernamePasswordAuthenticationToken(userId, "", grantedAuthorities);
    }

    /**
     * 사용자 정보를 응답 헤더에 추가
     */
    private void addUserInfoToHeaders(HttpServletRequest request, HttpServletResponse response, String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            // 사용자 ID 헤더 추가
            String userId = claims.getSubject();
            if (StringUtils.hasText(userId)) {
                response.setHeader("X-User-ID", userId);
            }
            
            // 고객 ID 헤더 추가 (있는 경우)
            String customerId = claims.get("customerId", String.class);
            if (StringUtils.hasText(customerId)) {
                response.setHeader("X-Customer-ID", customerId);
            }
            
            // 요청 ID 헤더 추가 (추적용)
            String requestId = request.getHeader("X-Request-ID");
            if (StringUtils.hasText(requestId)) {
                response.setHeader("X-Request-ID", requestId);
            }
        } catch (Exception e) {
            logger.warn("사용자 정보 헤더 추가 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 필터 적용 제외 경로 설정
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Health Check 및 문서화 API는 필터 제외
        return path.startsWith("/actuator/") || 
               path.startsWith("/v3/api-docs") || 
               path.startsWith("/swagger-ui");
    }
}