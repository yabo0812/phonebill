package com.unicorn.phonebill.gateway.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Circuit Breaker Fallback 핸들러
 * 
 * Circuit Breaker가 Open 상태일 때 또는 서비스 호출이 실패했을 때 
 * 대체 응답을 제공하는 핸들러입니다.
 * 
 * 주요 기능:
 * - 서비스별 개별 fallback 응답
 * - 표준화된 오류 응답 형식
 * - 적절한 HTTP 상태 코드 반환
 * - 로깅 및 모니터링 지원
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
@Component
public class FallbackHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(FallbackHandler.class);

    /**
     * 인증 서비스 Fallback
     * 
     * @param request ServerRequest
     * @return ServerResponse
     */
    public Mono<ServerResponse> authServiceFallback(ServerRequest request) {
        logger.warn("Auth service fallback triggered - URI: {}", request.uri());
        
        String fallbackResponse = createFallbackResponse(
            "AUTH503",
            "인증 서비스가 일시적으로 사용할 수 없습니다",
            "잠시 후 다시 시도해 주세요",
            "auth-service"
        );
        
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fallbackResponse);
    }

    /**
     * 요금조회 서비스 Fallback
     * 
     * @param request ServerRequest
     * @return ServerResponse
     */
    public Mono<ServerResponse> billServiceFallback(ServerRequest request) {
        logger.warn("Bill service fallback triggered - URI: {}", request.uri());
        
        String fallbackResponse = createFallbackResponse(
            "BILL503",
            "요금조회 서비스가 일시적으로 사용할 수 없습니다",
            "시스템 점검 중입니다. 잠시 후 다시 시도해 주세요",
            "bill-service"
        );
        
        // 요금조회의 경우 캐시된 데이터 제공 가능한지 확인
        if (request.path().contains("/bills/menu")) {
            return provideCachedMenuData();
        }
        
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fallbackResponse);
    }

    /**
     * 상품변경 서비스 Fallback
     * 
     * @param request ServerRequest
     * @return ServerResponse
     */
    public Mono<ServerResponse> productServiceFallback(ServerRequest request) {
        logger.warn("Product service fallback triggered - URI: {}", request.uri());
        
        String fallbackResponse = createFallbackResponse(
            "PROD503",
            "상품변경 서비스가 일시적으로 사용할 수 없습니다",
            "시스템 점검 중입니다. 고객센터로 문의하시거나 잠시 후 다시 시도해 주세요",
            "product-service"
        );
        
        // 상품변경 요청의 경우 더 신중한 처리 필요
        if (request.method().name().equals("POST")) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createCriticalServiceFallback("상품변경"));
        }
        
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fallbackResponse);
    }

    /**
     * KOS Mock 서비스 Fallback
     * 
     * @param request ServerRequest
     * @return ServerResponse
     */
    public Mono<ServerResponse> kosServiceFallback(ServerRequest request) {
        logger.warn("KOS service fallback triggered - URI: {}", request.uri());
        
        String fallbackResponse = createFallbackResponse(
            "KOS503",
            "외부 연동 시스템이 일시적으로 사용할 수 없습니다",
            "통신사 시스템 점검 중입니다. 잠시 후 다시 시도해 주세요",
            "kos-mock-service"
        );
        
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fallbackResponse);
    }

    /**
     * 일반 Fallback (알 수 없는 서비스)
     * 
     * @param request ServerRequest
     * @return ServerResponse
     */
    public Mono<ServerResponse> genericFallback(ServerRequest request) {
        logger.warn("Generic fallback triggered - URI: {}", request.uri());
        
        String fallbackResponse = createFallbackResponse(
            "SYS503",
            "서비스가 일시적으로 사용할 수 없습니다",
            "시스템 점검 중입니다. 잠시 후 다시 시도해 주세요",
            "unknown-service"
        );
        
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fallbackResponse);
    }

    /**
     * 표준 Fallback 응답 생성
     * 
     * @param errorCode 오류 코드
     * @param message 사용자 메시지
     * @param details 상세 설명
     * @param service 서비스명
     * @return JSON 형식 응답
     */
    private String createFallbackResponse(String errorCode, String message, String details, String service) {
        return String.format(
            "{\n" +
            "  \"success\": false,\n" +
            "  \"error\": {\n" +
            "    \"code\": \"%s\",\n" +
            "    \"message\": \"%s\",\n" +
            "    \"details\": \"%s\",\n" +
            "    \"service\": \"%s\",\n" +
            "    \"timestamp\": \"%s\",\n" +
            "    \"retry_after\": \"30\"\n" +
            "  }\n" +
            "}",
            errorCode,
            message,
            details,
            service,
            Instant.now().toString()
        );
    }

    /**
     * 중요 서비스 Fallback 응답 생성
     * 
     * @param serviceName 서비스명
     * @return JSON 형식 응답
     */
    private String createCriticalServiceFallback(String serviceName) {
        return String.format(
            "{\n" +
            "  \"success\": false,\n" +
            "  \"error\": {\n" +
            "    \"code\": \"CRITICAL_SERVICE_UNAVAILABLE\",\n" +
            "    \"message\": \"%s 서비스가 현재 이용할 수 없습니다\",\n" +
            "    \"details\": \"중요한 작업이므로 시스템이 안정된 후 다시 시도해 주시기 바랍니다\",\n" +
            "    \"action\": \"CONTACT_SUPPORT\",\n" +
            "    \"support_phone\": \"1588-0000\",\n" +
            "    \"timestamp\": \"%s\",\n" +
            "    \"retry_after\": \"300\"\n" +
            "  }\n" +
            "}",
            serviceName,
            Instant.now().toString()
        );
    }

    /**
     * 캐시된 메뉴 데이터 제공
     * 
     * 요금조회 메뉴는 변경이 적으므로 캐시된 데이터를 제공할 수 있습니다.
     * 
     * @return ServerResponse
     */
    private Mono<ServerResponse> provideCachedMenuData() {
        String cachedMenuResponse = 
            "{\n" +
            "  \"success\": true,\n" +
            "  \"message\": \"캐시된 메뉴 정보입니다\",\n" +
            "  \"data\": {\n" +
            "    \"menus\": [\n" +
            "      {\n" +
            "        \"id\": \"bill_inquiry\",\n" +
            "        \"name\": \"요금조회\",\n" +
            "        \"description\": \"현재 요금 정보를 조회합니다\",\n" +
            "        \"available\": true\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"cache_info\": {\n" +
            "    \"cached\": true,\n" +
            "    \"timestamp\": \"" + Instant.now().toString() + "\",\n" +
            "    \"note\": \"서비스 점검 중이므로 캐시된 정보를 제공합니다\"\n" +
            "  }\n" +
            "}";
        
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Cache", "HIT")
                .header("X-Cache-Reason", "SERVICE_UNAVAILABLE")
                .bodyValue(cachedMenuResponse);
    }

    /**
     * Rate Limit 초과 Fallback
     * 
     * @param request ServerRequest
     * @return ServerResponse
     */
    public Mono<ServerResponse> rateLimitFallback(ServerRequest request) {
        logger.warn("Rate limit exceeded fallback - URI: {}", request.uri());
        
        String fallbackResponse = createFallbackResponse(
            "RATE_LIMIT_EXCEEDED",
            "요청 한도를 초과했습니다",
            "잠시 후 다시 시도해 주세요",
            "rate-limiter"
        );
        
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Retry-After", "60")
                .bodyValue(fallbackResponse);
    }
}