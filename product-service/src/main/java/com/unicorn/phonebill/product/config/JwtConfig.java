package com.unicorn.phonebill.product.config;

import com.phonebill.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * JWT 설정 프로퍼티
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {
    
    private String secret;
    private long accessTokenValidity = 1800000; // 30분 (milliseconds)
    private long refreshTokenValidity = 86400000; // 24시간 (milliseconds)
    private String issuer = "phonebill-auth-service";
    
    /**
     * Access Token 만료 시간 (초 단위)
     */
    public int getAccessTokenValidityInSeconds() {
        return (int) (accessTokenValidity / 1000);
    }
    
    /**
     * Refresh Token 만료 시간 (초 단위)
     */
    public int getRefreshTokenValidityInSeconds() {
        return (int) (refreshTokenValidity / 1000);
    }
    
    /**
     * JwtTokenProvider 빈 정의
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long tokenValidityInMilliseconds) {
        long tokenValidityInSeconds = tokenValidityInMilliseconds / 1000;
        return new JwtTokenProvider(secret, tokenValidityInSeconds);
    }
}