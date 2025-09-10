package com.unicorn.phonebill.gateway.config;

import com.unicorn.phonebill.gateway.filter.JwtAuthenticationGatewayFilterFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cloud Gateway 라우팅 및 CORS 설정
 * 
 * 마이크로서비스별 라우팅 규칙과 CORS 정책을 정의합니다.
 * 
 * 라우팅 구성:
 * - /api/auth/** -> /api/v1/auth/** (user-service)
 * - /api/bills/** -> /api/v1/bills/** (bill-service) 
 * - /api/products/** -> /products/** (product-service)
 * - /api/kos/** -> /kos/** (kos-mock)
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
@Configuration
public class GatewayConfig {

    private final JwtAuthenticationGatewayFilterFactory jwtAuthFilter;

    
    @Value("${services.user-service.url}")
    private String userServiceUrl;
    
    @Value("${services.bill-service.url}")
    private String billServiceUrl;
    
    @Value("${services.product-service.url}")
    private String productServiceUrl;
    
    @Value("${services.kos-mock.url}")
    private String kosMockUrl;

    public GatewayConfig(JwtAuthenticationGatewayFilterFactory jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * 라우팅 규칙 정의
     * 
     * 각 마이크로서비스로의 라우팅 규칙과 필터를 설정합니다.
     * JWT 인증이 필요한 경로와 불필요한 경로를 구분하여 처리합니다.
     * 
     * @param builder RouteLocatorBuilder
     * @return RouteLocator
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service 라우팅 (인증 불필요)
                .route("user-service-login", r -> r
                        .path("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh")
                        .and()
                        .method("POST")
                        .uri(userServiceUrl))
                
                // Auth Service 라우팅 (인증 필요)
                .route("user-service-authenticated", r -> r
                        .path("/api/v1/auth/**", "/api/v1/users/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthenticationGatewayFilterFactory.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("user-service-cb")
                                        .setFallbackUri("forward:/fallback/auth"))
                                .retry(retry -> retry
                                        .setRetries(3)
                                        .setBackoff(java.time.Duration.ofSeconds(2), java.time.Duration.ofSeconds(10), 2, true)))
                        .uri(userServiceUrl))
                
                // Bill-Inquiry Service 라우팅 (인증 필요)
                .route("bill-service", r -> r
                        .path("/api/v1/bills/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthenticationGatewayFilterFactory.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("bill-service-cb")
                                        .setFallbackUri("forward:/fallback/bill"))
                                .retry(retry -> retry
                                        .setRetries(3)
                                        .setBackoff(java.time.Duration.ofSeconds(2), java.time.Duration.ofSeconds(10), 2, true))
                                )
                        .uri(billServiceUrl))
                
                // Product-Change Service 라우팅 (인증 필요)
                .route("product-service", r -> r
                        .path("/api/v1/products/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthenticationGatewayFilterFactory.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("product-service-cb")
                                        .setFallbackUri("forward:/fallback/product"))
                                .retry(retry -> retry
                                        .setRetries(3)
                                        .setBackoff(java.time.Duration.ofSeconds(2), java.time.Duration.ofSeconds(10), 2, true))
                                )
                        .uri(productServiceUrl))
                
                // KOS Mock Service 라우팅 (인증 불필요 - 목업용)
                .route("kos-mock-service", r -> r
                        .path("/api/v1/kos/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("kos-mock-cb")
                                        .setFallbackUri("forward:/fallback/kos"))
                                .retry(retry -> retry
                                        .setRetries(2)
                                        .setBackoff(java.time.Duration.ofSeconds(1), java.time.Duration.ofSeconds(5), 2, true))
                        )
                        .uri(kosMockUrl))

                // 주의: Gateway 자체 엔드포인트는 라우팅하지 않음
                // Health Check와 Swagger UI는 Spring Boot에서 직접 제공
                
                .build();
    }

    /**
     * CORS 설정은 application.yml의 globalcors에서 관리
     * add-to-simple-url-handler-mapping: true 설정으로
     * 라우트 predicate와 매치되지 않는 OPTIONS 요청도 처리
     */
}