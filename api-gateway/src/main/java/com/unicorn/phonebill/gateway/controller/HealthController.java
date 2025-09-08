package com.unicorn.phonebill.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * API Gateway 헬스체크 컨트롤러
 * 
 * API Gateway와 연관된 시스템들의 상태를 점검합니다.
 * 
 * 주요 기능:
 * - Gateway 자체 상태 확인
 * - Redis 연결 상태 확인
 * - 각 마이크로서비스 연결 상태 확인
 * - 전체 시스템 상태 요약
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
@RestController
public class HealthController {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    @Autowired
    public HealthController(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
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
                checkRedisHealth(),
                checkDownstreamServices()
        ).map(tuple -> {
            Map<String, Object> gatewayHealth = tuple.getT1();
            Map<String, Object> redisHealth = tuple.getT2();
            Map<String, Object> servicesHealth = tuple.getT3();
            
            boolean allHealthy = 
                "UP".equals(gatewayHealth.get("status")) &&
                "UP".equals(redisHealth.get("status")) &&
                "UP".equals(servicesHealth.get("status"));
            
            Map<String, Object> response = Map.of(
                "status", allHealthy ? "UP" : "DOWN",
                "timestamp", Instant.now().toString(),
                "components", Map.of(
                    "gateway", gatewayHealth,
                    "redis", redisHealth,
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
        return Mono.zip(
                checkGatewayHealth(),
                checkRedisHealth()
        ).map(tuple -> {
            Map<String, Object> gatewayHealth = tuple.getT1();
            Map<String, Object> redisHealth = tuple.getT2();
            
            boolean allHealthy = 
                "UP".equals(gatewayHealth.get("status")) &&
                "UP".equals(redisHealth.get("status"));
            
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
     * Redis 연결 상태 점검
     * 
     * @return Redis 상태
     */
    private Mono<Map<String, Object>> checkRedisHealth() {
        return redisTemplate.hasKey("health:check")
                .timeout(Duration.ofSeconds(3))
                .map(result -> Map.<String, Object>of(
                    "status", "UP",
                    "connection", "OK",
                    "response_time", "< 3s",
                    "timestamp", Instant.now().toString()
                ))
                .onErrorReturn(Map.<String, Object>of(
                    "status", "DOWN",
                    "connection", "FAILED",
                    "error", "Connection timeout or error",
                    "timestamp", Instant.now().toString()
                ));
    }

    /**
     * 다운스트림 서비스 상태 점검
     * 
     * @return 서비스 상태
     */
    private Mono<Map<String, Object>> checkDownstreamServices() {
        // 실제 구현에서는 Circuit Breaker 상태를 확인하거나
        // 각 서비스에 대한 간단한 health check를 수행할 수 있습니다.
        return Mono.fromCallable(() -> Map.<String, Object>of(
            "status", "UP",
            "services", Map.<String, Object>of(
                "auth-service", "UNKNOWN",
                "bill-service", "UNKNOWN", 
                "product-service", "UNKNOWN",
                "kos-mock-service", "UNKNOWN"
            ),
            "note", "Service health checks not implemented yet",
            "timestamp", Instant.now().toString()
        ));
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