package com.unicorn.phonebill.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API Gateway 헬스체크 컨트롤러
 * 
 * API Gateway와 연관된 시스템들의 상태를 점검합니다.
 * 
 * 주요 기능:
 * - Gateway 자체 상태 확인
 * - 각 마이크로서비스 연결 상태 확인
 * - 전체 시스템 상태 요약
 * 
 * Note: Redis는 API Gateway에서 사용하지 않으므로 Redis health check 제거
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
@RestController
public class HealthController {

    private final WebClient webClient;
    
    @Value("${services.auth-service.url:http://localhost:8081}")
    private String authServiceUrl;
    
    @Value("${services.bill-service.url:http://localhost:8082}")
    private String billServiceUrl;
    
    @Value("${services.product-service.url:http://localhost:8083}")
    private String productServiceUrl;

    public HealthController() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
    }

    /**
     * 기본 헬스체크 엔드포인트
     * 
     * @return 상태 응답
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return checkSystemHealth()
                .map(healthStatus -> {
                    HttpStatus status = healthStatus.get("status").equals("UP") 
                            ? HttpStatus.OK 
                            : HttpStatus.SERVICE_UNAVAILABLE;
                    
                    return ResponseEntity.status(status).body(healthStatus);
                })
                .onErrorReturn(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.<String, Object>of(
                                "status", "DOWN",
                                "error", "Health check failed",
                                "timestamp", Instant.now().toString()
                            ))
                );
    }

    /**
     * 상세 헬스체크 엔드포인트
     * 
     * @return 상세 상태 정보
     */
    @GetMapping("/health/detailed")
    public Mono<ResponseEntity<Map<String, Object>>> detailedHealth() {
        return Mono.zip(
                checkGatewayHealth(),
                checkDownstreamServices()
        ).map(tuple -> {
            Map<String, Object> gatewayHealth = tuple.getT1();
            Map<String, Object> servicesHealth = tuple.getT2();
            
            boolean allHealthy = 
                "UP".equals(gatewayHealth.get("status")) &&
                "UP".equals(servicesHealth.get("status"));
            
            Map<String, Object> response = Map.of(
                "status", allHealthy ? "UP" : "DOWN",
                "timestamp", Instant.now().toString(),
                "components", Map.of(
                    "gateway", gatewayHealth,
                    "services", servicesHealth
                )
            );
            
            HttpStatus status = allHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(response);
        }).onErrorReturn(
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.<String, Object>of(
                        "status", "DOWN",
                        "error", "Detailed health check failed",
                        "timestamp", Instant.now().toString()
                    ))
        );
    }

    /**
     * 간단한 상태 확인 엔드포인트
     * 
     * @return 상태 응답
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<Map<String, Object>>> status() {
        return Mono.just(ResponseEntity.ok(Map.<String, Object>of(
            "status", "UP",
            "service", "API Gateway",
            "timestamp", Instant.now().toString(),
            "version", "1.0.0"
        )));
    }

    /**
     * 전체 시스템 상태 점검
     * 
     * @return 시스템 상태
     */
    private Mono<Map<String, Object>> checkSystemHealth() {
        return checkGatewayHealth()
                .map(gatewayHealth -> {
                    boolean allHealthy = "UP".equals(gatewayHealth.get("status"));
                    
                    return Map.<String, Object>of(
                        "status", allHealthy ? "UP" : "DOWN",
                        "timestamp", Instant.now().toString(),
                        "version", "1.0.0",
                        "uptime", getUptime()
                    );
                });
    }

    /**
     * Gateway 자체 상태 점검
     * 
     * @return Gateway 상태
     */
    private Mono<Map<String, Object>> checkGatewayHealth() {
        return Mono.fromCallable(() -> {
            // 메모리 사용량 확인
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsage = (double) usedMemory / totalMemory * 100;
            
            return Map.<String, Object>of(
                "status", memoryUsage < 90 ? "UP" : "DOWN",
                "memory", Map.<String, Object>of(
                    "used", usedMemory,
                    "total", totalMemory,
                    "usage_percent", String.format("%.2f%%", memoryUsage)
                ),
                "threads", Thread.activeCount(),
                "timestamp", Instant.now().toString()
            );
        });
    }


    /**
     * 다운스트림 서비스 상태 점검
     * 
     * @return 서비스 상태
     */
    private Mono<Map<String, Object>> checkDownstreamServices() {
        // 모든 서비스의 health check를 병렬로 수행
        Mono<Map<String, Object>> authCheck = checkServiceHealth("auth-service", authServiceUrl);
        Mono<Map<String, Object>> billCheck = checkServiceHealth("bill-service", billServiceUrl);
        Mono<Map<String, Object>> productCheck = checkServiceHealth("product-service", productServiceUrl);
        
        return Mono.zip(authCheck, billCheck, productCheck)
                .map(tuple -> {
                    Map<String, Object> authResult = tuple.getT1();
                    Map<String, Object> billResult = tuple.getT2();
                    Map<String, Object> productResult = tuple.getT3();
                    
                    // 전체 서비스 상태 계산
                    boolean anyServiceDown = 
                        "DOWN".equals(authResult.get("status")) ||
                        "DOWN".equals(billResult.get("status")) ||
                        "DOWN".equals(productResult.get("status"));
                    
                    Map<String, Object> services = new ConcurrentHashMap<>();
                    services.put("auth-service", authResult);
                    services.put("bill-service", billResult);
                    services.put("product-service", productResult);
                    
                    return Map.<String, Object>of(
                        "status", anyServiceDown ? "DEGRADED" : "UP",
                        "services", services,
                        "timestamp", Instant.now().toString(),
                        "summary", String.format("Total services: 3, Up: %d, Down: %d", 
                            countServicesByStatus(services, "UP"),
                            countServicesByStatus(services, "DOWN"))
                    );
                })
                .onErrorReturn(Map.of(
                    "status", "DOWN",
                    "error", "Failed to check downstream services",
                    "timestamp", Instant.now().toString()
                ));
    }

    /**
     * 개별 서비스 health check
     * 
     * @param serviceName 서비스 이름
     * @param serviceUrl 서비스 URL
     * @return 서비스 상태
     */
    private Mono<Map<String, Object>> checkServiceHealth(String serviceName, String serviceUrl) {
        return webClient.get()
                .uri(serviceUrl + "/actuator/health")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(3))
                .map(response -> {
                    String status = (String) response.getOrDefault("status", "UNKNOWN");
                    return Map.<String, Object>of(
                        "status", "UP".equals(status) ? "UP" : "DOWN",
                        "url", serviceUrl,
                        "response_time", "< 3s",
                        "details", response,
                        "timestamp", Instant.now().toString()
                    );
                })
                .onErrorReturn(Map.of(
                    "status", "DOWN",
                    "url", serviceUrl,
                    "error", "Connection failed or timeout",
                    "timestamp", Instant.now().toString()
                ));
    }

    /**
     * 서비스 상태별 개수 계산
     * 
     * @param services 서비스 맵
     * @param status 확인할 상태
     * @return 해당 상태의 서비스 개수
     */
    private long countServicesByStatus(Map<String, Object> services, String status) {
        return services.values().stream()
                .filter(service -> service instanceof Map)
                .map(service -> (Map<String, Object>) service)
                .filter(service -> status.equals(service.get("status")))
                .count();
    }

    /**
     * 애플리케이션 업타임 계산
     * 
     * @return 업타임 문자열
     */
    private String getUptime() {
        long uptimeMs = System.currentTimeMillis() - getStartTime();
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * 애플리케이션 시작 시간 반환 (임시 구현)
     * 
     * @return 시작 시간 (밀리초)
     */
    private long getStartTime() {
        // 실제 구현에서는 ApplicationContext에서 시작 시간을 가져와야 합니다.
        return System.currentTimeMillis() - 300000; // 임시로 5분 전으로 설정
    }
}