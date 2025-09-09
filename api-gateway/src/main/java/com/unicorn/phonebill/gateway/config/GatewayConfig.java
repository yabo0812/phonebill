package com.unicorn.phonebill.gateway.config;

import com.unicorn.phonebill.gateway.filter.JwtAuthenticationGatewayFilterFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Cloud Gateway 라우팅 및 CORS 설정
 * 
 * 마이크로서비스별 라우팅 규칙과 CORS 정책을 정의합니다.
 * 
 * 라우팅 구성:
 * - /auth/** -> auth-service (인증 서비스)
 * - /bills/** -> bill-service (요금조회 서비스) 
 * - /products/** -> product-service (상품변경 서비스)
 * - /kos/** -> kos-mock (KOS 목업 서비스)
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
@Configuration
public class GatewayConfig {

    private final JwtAuthenticationGatewayFilterFactory jwtAuthFilter;

    @Value("${cors.allowed-origins")
    private String allowedOrigins;

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
                        .path("/api/auth/login", "/api/auth/refresh")
                        .and()
                        .method("POST")
                        .filters(f -> f.rewritePath("/api/auth/(?<segment>.*)", "/auth/${segment}"))
                        .uri("lb://user-service"))
                
                // Auth Service 라우팅 (인증 필요)
                .route("user-service-authenticated", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .rewritePath("/api/auth/(?<segment>.*)", "/auth/${segment}")
                                .filter(jwtAuthFilter.apply(new JwtAuthenticationGatewayFilterFactory.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("user-service-cb")
                                        .setFallbackUri("forward:/fallback/auth"))
                                .retry(retry -> retry
                                        .setRetries(3)
                                        .setBackoff(java.time.Duration.ofSeconds(2), java.time.Duration.ofSeconds(10), 2, true)))
                        .uri("lb://user-service"))
                
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
                        .uri("lb://bill-service"))
                
                // Product-Change Service 라우팅 (인증 필요)
                .route("product-service", r -> r
                        .path("/products/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(new JwtAuthenticationGatewayFilterFactory.Config()))
                                .circuitBreaker(cb -> cb
                                        .setName("product-service-cb")
                                        .setFallbackUri("forward:/fallback/product"))
                                .retry(retry -> retry
                                        .setRetries(3)
                                        .setBackoff(java.time.Duration.ofSeconds(2), java.time.Duration.ofSeconds(10), 2, true))
                                )
                        .uri("lb://product-service"))
                
                // KOS Mock Service 라우팅 (내부 서비스용)
                .route("kos-mock-service", r -> r
                        .path("/kos/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("kos-mock-cb")
                                        .setFallbackUri("forward:/fallback/kos"))
                                .retry(retry -> retry
                                        .setRetries(5)
                                        .setBackoff(java.time.Duration.ofSeconds(1), java.time.Duration.ofSeconds(5), 2, true)))
                        .uri("lb://kos-mock-service"))
                
                // Health Check 라우팅 (인증 불필요)
                .route("health-check", r -> r
                        .path("/health", "/actuator/health")
                        .uri("http://localhost:8080"))
                
                // Swagger UI 라우팅 (개발환경에서만 사용)
                .route("swagger-ui", r -> r
                        .path("/swagger-ui/**", "/v3/api-docs/**")
                        .uri("http://localhost:8080"))
                
                .build();
    }

    /**
     * CORS 설정
     * 
     * 프론트엔드에서 API Gateway로의 크로스 오리진 요청을 허용합니다.
     * 개발/운영 환경에 따라 허용 오리진을 다르게 설정합니다.
     * 
     * @return CorsWebFilter
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // 환경변수에서 허용할 Origin 패턴 설정
        String[] origins = allowedOrigins.split(",");
        corsConfig.setAllowedOriginPatterns(Arrays.asList(origins));

        // 허용할 HTTP 메서드
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"
        ));
        
        // 허용할 헤더
        corsConfig.setAllowedHeaders(Arrays.asList(
                "Authorization", 
                "Content-Type", 
                "X-Requested-With",
                "X-Request-ID",
                "X-User-Agent"
        ));
        
        // 노출할 헤더 (클라이언트가 접근 가능한 헤더)
        corsConfig.setExposedHeaders(Arrays.asList(
                "X-Request-ID",
                "X-Response-Time",
                "X-Rate-Limit-Remaining"
        ));
        
        // 자격 증명 허용 (쿠키, Authorization 헤더 등)
        corsConfig.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (초)
        corsConfig.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}