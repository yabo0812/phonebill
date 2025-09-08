package com.unicorn.phonebill.gateway.filter;

import com.unicorn.phonebill.gateway.service.JwtTokenService;
import com.unicorn.phonebill.gateway.dto.TokenValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * JWT 인증 Gateway Filter Factory
 * 
 * Spring Cloud Gateway에서 JWT 토큰 기반 인증을 처리하는 필터입니다.
 * Authorization 헤더의 Bearer 토큰을 검증하고, 유효하지 않은 경우 요청을 차단합니다.
 * 
 * 주요 기능:
 * - JWT 토큰 유효성 검증
 * - 토큰 만료 검사
 * - 사용자 정보 추출 및 헤더 전달
 * - 인증 실패 시 적절한 HTTP 응답 반환
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
@Component
public class JwtAuthenticationGatewayFilterFactory 
        extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationGatewayFilterFactory.class);
    
    private final JwtTokenService jwtTokenService;
    
    public JwtAuthenticationGatewayFilterFactory(JwtTokenService jwtTokenService) {
        super(Config.class);
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String requestPath = request.getPath().value();
            String requestId = request.getHeaders().getFirst("X-Request-ID");
            
            logger.debug("JWT Authentication Filter - Path: {}, Request-ID: {}", requestPath, requestId);
            
            // Authorization 헤더 추출
            String authHeader = request.getHeaders().getFirst("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Missing or invalid Authorization header - Path: {}, Request-ID: {}", 
                           requestPath, requestId);
                return handleAuthenticationError(exchange, "인증 토큰이 없습니다", HttpStatus.UNAUTHORIZED);
            }
            
            // Bearer 토큰 추출
            String token = authHeader.substring(7);
            
            // JWT 토큰 검증 (비동기)
            return jwtTokenService.validateToken(token)
                    .flatMap(validationResult -> {
                        if (validationResult.isValid()) {
                            // 인증 성공 - 사용자 정보를 헤더에 추가하여 하위 서비스로 전달
                            ServerHttpRequest modifiedRequest = request.mutate()
                                    .header("X-User-ID", validationResult.getUserId())
                                    .header("X-User-Role", validationResult.getUserRole())
                                    .header("X-Token-Expires-At", String.valueOf(validationResult.getExpiresAt()))
                                    .header("X-Request-ID", requestId != null ? requestId : generateRequestId())
                                    .build();
                            
                            logger.debug("JWT Authentication success - User: {}, Role: {}, Path: {}, Request-ID: {}", 
                                        validationResult.getUserId(), validationResult.getUserRole(), 
                                        requestPath, requestId);
                            
                            return chain.filter(exchange.mutate().request(modifiedRequest).build());
                        } else {
                            // 인증 실패
                            logger.warn("JWT Authentication failed - Reason: {}, Path: {}, Request-ID: {}", 
                                       validationResult.getFailureReason(), requestPath, requestId);
                            
                            HttpStatus status = determineHttpStatus(validationResult.getFailureReason());
                            return handleAuthenticationError(exchange, validationResult.getFailureReason(), status);
                        }
                    })
                    .onErrorResume(throwable -> {
                        logger.error("JWT Authentication error - Path: {}, Request-ID: {}, Error: {}", 
                                    requestPath, requestId, throwable.getMessage(), throwable);
                        
                        return handleAuthenticationError(exchange, "인증 처리 중 오류가 발생했습니다", 
                                                       HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        };
    }

    /**
     * 실패 원인에 따른 HTTP 상태 코드 결정
     * 
     * @param failureReason 실패 원인
     * @return HTTP 상태 코드
     */
    private HttpStatus determineHttpStatus(String failureReason) {
        if (failureReason == null) {
            return HttpStatus.UNAUTHORIZED;
        }
        
        if (failureReason.contains("만료")) {
            return HttpStatus.UNAUTHORIZED;
        } else if (failureReason.contains("권한")) {
            return HttpStatus.FORBIDDEN;
        } else if (failureReason.contains("형식")) {
            return HttpStatus.BAD_REQUEST;
        }
        
        return HttpStatus.UNAUTHORIZED;
    }

    /**
     * 인증 오류 응답 처리
     * 
     * @param exchange ServerWebExchange
     * @param message 오류 메시지
     * @param status HTTP 상태 코드
     * @return Mono<Void>
     */
    private Mono<Void> handleAuthenticationError(
            org.springframework.web.server.ServerWebExchange exchange, 
            String message, 
            HttpStatus status) {
        
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        
        // 표준 오류 응답 형식
        String jsonResponse = String.format(
            "{\n" +
            "  \"success\": false,\n" +
            "  \"error\": {\n" +
            "    \"code\": \"AUTH%03d\",\n" +
            "    \"message\": \"%s\",\n" +
            "    \"timestamp\": \"%s\"\n" +
            "  }\n" +
            "}",
            status.value(),
            message,
            java.time.Instant.now().toString()
        );
        
        DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 요청 ID 생성
     * 
     * @return 생성된 요청 ID
     */
    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + 
               (int)(Math.random() * 1000);
    }

    /**
     * Filter 설정 클래스
     */
    public static class Config {
        // 필요에 따라 설정 프로퍼티 추가 가능
        private boolean enabled = true;
        private String[] excludePaths = {};
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String[] getExcludePaths() {
            return excludePaths;
        }
        
        public void setExcludePaths(String[] excludePaths) {
            this.excludePaths = excludePaths;
        }
    }
}