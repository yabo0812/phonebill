package com.phonebill.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

import com.phonebill.user.config.AuthConfig;
import com.phonebill.user.config.JwtConfig;

/**
 * User Service (Auth Service) 메인 애플리케이션
 * 
 * 주요 기능:
 * - 사용자 인증/인가 (JWT 기반)
 * - 사용자 세션 관리 (Redis)
 * - 로그인/로그아웃 처리
 * - 권한 관리 및 검증
 * - 계정 잠금/해제 관리
 */
@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties({JwtConfig.class, AuthConfig.class})
@ComponentScan(basePackages = {"com.phonebill.user", "com.phonebill.common"})
public class UserServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}