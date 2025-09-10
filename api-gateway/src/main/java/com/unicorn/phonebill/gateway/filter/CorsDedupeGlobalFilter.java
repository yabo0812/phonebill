package com.unicorn.phonebill.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Global Filter for removing duplicate CORS headers
 * 
 * 이 필터는 백엔드 서비스에서 오는 CORS 헤더를 제거하여
 * API Gateway의 GlobalCors 설정만 사용하도록 합니다.
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
@Slf4j
@Component
public class CorsDedupeGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> CORS_HEADERS = Arrays.asList(
        "Access-Control-Allow-Origin",
        "Access-Control-Allow-Methods", 
        "Access-Control-Allow-Headers",
        "Access-Control-Allow-Credentials",
        "Access-Control-Expose-Headers",
        "Access-Control-Max-Age"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        log.info("=== CorsDedupeGlobalFilter 시작 - Path: {}", path);
        
        // Response를 감싸서 헤더 수정 가능하게 만들기
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders originalHeaders = super.getHeaders();
                HttpHeaders filteredHeaders = new HttpHeaders();
                
                // 원본 헤더를 안전하게 복사하면서 CORS 헤더는 제외
                final int[] removedCount = {0};
                originalHeaders.forEach((key, values) -> {
                    if (CORS_HEADERS.contains(key)) {
                        removedCount[0]++;
                        log.info("CORS 헤더 제거: {} = {}", key, values);
                    } else {
                        try {
                            filteredHeaders.addAll(key, values);
                        } catch (Exception e) {
                            log.warn("헤더 추가 실패: {} = {}, 에러: {}", key, values, e.getMessage());
                        }
                    }
                });
                
                if (removedCount[0] > 0) {
                    log.info("=== CorsDedupeGlobalFilter 완료 - 제거된 헤더 수: {}", removedCount[0]);
                }
                
                return filteredHeaders;
            }
        };
        
        // 수정된 response로 exchange 생성
        ServerWebExchange mutatedExchange = exchange.mutate().response(decoratedResponse).build();
        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return -1; // 높은 우선순위로 설정
    }
}