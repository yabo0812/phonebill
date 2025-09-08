package com.unicorn.phonebill.gateway.config;

import com.unicorn.phonebill.gateway.handler.FallbackHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Web 설정 및 라우터 함수 정의
 * 
 * Spring WebFlux의 함수형 라우팅을 사용하여 Fallback 엔드포인트를 정의합니다.
 * Circuit Breaker에서 호출할 Fallback 경로를 설정합니다.
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
@Configuration
public class WebConfig {

    private final FallbackHandler fallbackHandler;

    public WebConfig(FallbackHandler fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
    }

    /**
     * Fallback 라우터 함수
     * 
     * Circuit Breaker에서 사용할 Fallback 엔드포인트를 정의합니다.
     * 
     * @return RouterFunction
     */
    @Bean
    public RouterFunction<ServerResponse> fallbackRouterFunction() {
        return RouterFunctions.route()
                // 인증 서비스 Fallback
                .GET("/fallback/auth", fallbackHandler::authServiceFallback)
                .POST("/fallback/auth", fallbackHandler::authServiceFallback)
                
                // 요금조회 서비스 Fallback
                .GET("/fallback/bill", fallbackHandler::billServiceFallback)
                .POST("/fallback/bill", fallbackHandler::billServiceFallback)
                
                // 상품변경 서비스 Fallback
                .GET("/fallback/product", fallbackHandler::productServiceFallback)
                .POST("/fallback/product", fallbackHandler::productServiceFallback)
                .PUT("/fallback/product", fallbackHandler::productServiceFallback)
                
                // KOS Mock 서비스 Fallback
                .GET("/fallback/kos", fallbackHandler::kosServiceFallback)
                .POST("/fallback/kos", fallbackHandler::kosServiceFallback)
                
                // Rate Limit Fallback
                .GET("/fallback/ratelimit", fallbackHandler::rateLimitFallback)
                .POST("/fallback/ratelimit", fallbackHandler::rateLimitFallback)
                
                // 일반 Fallback (기타 모든 경로)
                .GET("/fallback/**", fallbackHandler::genericFallback)
                .POST("/fallback/**", fallbackHandler::genericFallback)
                
                .build();
    }
}