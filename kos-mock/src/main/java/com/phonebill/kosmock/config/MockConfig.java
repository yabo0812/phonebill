package com.phonebill.kosmock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * KOS Mock 설정
 */
@Configuration
@ConfigurationProperties(prefix = "kos.mock")
@Data
public class MockConfig {
    
    /**
     * Mock 응답 지연 시간 (밀리초)
     */
    private long responseDelay = 500;
    
    /**
     * Mock 실패율 (0.0 ~ 1.0)
     */
    private double failureRate = 0.0;
    
    /**
     * 최대 재시도 횟수
     */
    private int maxRetryCount = 3;
    
    /**
     * 타임아웃 시간 (밀리초)
     */
    private long timeoutMs = 30000;
    
    /**
     * 디버그 모드 활성화 여부
     */
    private boolean debugMode = false;
}