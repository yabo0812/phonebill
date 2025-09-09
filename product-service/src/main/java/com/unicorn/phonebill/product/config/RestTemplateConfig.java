package com.unicorn.phonebill.product.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 설정 클래스
 * 
 * KOS 시스템 연동을 위한 HTTP 클라이언트 구성
 * - Connection Pool 설정
 * - Timeout 설정  
 * - 재시도 및 회로 차단기와 연동
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-09
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final KosProperties kosProperties;

    /**
     * KOS 연동용 RestTemplate 빈 생성
     * 
     * @param builder RestTemplate 빌더
     * @return 설정된 RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        log.info("RestTemplate 빈 생성 - 연결 타임아웃: {}ms, 읽기 타임아웃: {}ms", 
                kosProperties.getConnectTimeout(), kosProperties.getReadTimeout());

        return builder
                // 타임아웃 설정
                .setConnectTimeout(Duration.ofMillis(kosProperties.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(kosProperties.getReadTimeout()))
                
                // RestTemplate 생성
                .build();
    }
}