package com.phonebill.bill.config;

import com.phonebill.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 설정
 * 
 * Bill Service의 JWT 토큰 검증을 위한 설정
 * User Service와 동일한 시크릿 키를 사용하여 토큰 검증
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-09
 */
@Configuration
public class JwtConfig {

    /**
     * JwtTokenProvider 빈 생성
     * 
     * @param secret JWT 시크릿 키
     * @param expirationInSeconds JWT 만료 시간 (초)
     * @return JwtTokenProvider 인스턴스
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret:dev-jwt-secret-key-for-development-only}") String secret,
            @Value("${jwt.access-token-validity:86400000}") long expirationInMillis) {
        
        // 만료 시간을 초 단위로 변환
        long expirationInSeconds = expirationInMillis / 1000;
        
        return new JwtTokenProvider(secret, expirationInSeconds);
    }
}