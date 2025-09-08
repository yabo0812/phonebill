package com.unicorn.phonebill.gateway.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * API Gateway Health Indicator
 * 
 * Spring Boot Actuator의 HealthIndicator 인터페이스를 구현하여
 * API Gateway의 상태를 점검합니다.
 * 
 * 주요 점검 항목:
 * - 메모리 사용률
 * - 시스템 상태
 * - 스레드 상태
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
@Component("gateway")
public class GatewayHealthIndicator implements HealthIndicator {

    /**
     * Actuator HealthIndicator 인터페이스 구현
     * 
     * @return Health 상태
     */
    @Override
    public Health health() {
        try {
            // 메모리 사용률 확인
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsage = (double) usedMemory / totalMemory * 100;

            Health.Builder healthBuilder = Health.up()
                    .withDetail("service", "API Gateway")
                    .withDetail("timestamp", Instant.now().toString())
                    .withDetail("memory", String.format("%.2f%%", memoryUsage))
                    .withDetail("threads", Thread.activeCount())
                    .withDetail("system", "Gateway routing only");

            // 메모리 사용률이 90% 이상이면 DOWN
            if (memoryUsage >= 90.0) {
                return healthBuilder.down()
                        .withDetail("status", "Memory usage too high")
                        .build();
            }

            return healthBuilder.build();
                    
        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "API Gateway")
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        }
    }
}