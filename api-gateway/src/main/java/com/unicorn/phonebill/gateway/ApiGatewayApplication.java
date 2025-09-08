package com.unicorn.phonebill.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * API Gateway 애플리케이션 메인 클래스
 * 
 * Spring Cloud Gateway를 사용하여 마이크로서비스들의 단일 진입점 역할을 담당합니다.
 * 
 * 주요 기능:
 * - JWT 토큰 기반 인증/인가
 * - 서비스별 라우팅 (user-service, bill-service, product-service, kos-mock)  
 * - CORS 설정
 * - Circuit Breaker 패턴 적용
 * - Rate Limiting
 * - API 문서화 통합
 * - 모니터링 및 헬스체크
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
})
@EnableConfigurationProperties(GatewayLoadBalancerProperties.class)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        // 시스템 프로퍼티 설정 (성능 최적화)
        System.setProperty("spring.main.lazy-initialization", "true");
        System.setProperty("reactor.bufferSize.small", "256");
        
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}