package com.unicorn.phonebill.gateway.config;

import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;

/**
 * WebFlux 설정
 * 
 * Spring Cloud Gateway에서 필요한 WebFlux 관련 빈들을 정의합니다.
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
@Configuration
public class WebFluxConfig {

    /**
     * ServerCodecConfigurer 빈 정의
     * 
     * Spring Cloud Gateway가 요구하는 ServerCodecConfigurer를 직접 정의합니다.
     * 
     * @return ServerCodecConfigurer
     */
    @Bean
    public ServerCodecConfigurer serverCodecConfigurer() {
        return ServerCodecConfigurer.create();
    }

    /**
     * CodecCustomizer 빈 정의 (선택적)
     * 
     * 필요한 경우 코덱을 커스터마이징할 수 있습니다.
     * 
     * @return CodecCustomizer
     */
    @Bean
    public CodecCustomizer codecCustomizer() {
        return configurer -> {
            // 최대 메모리 크기 설정 (기본값: 256KB)
            configurer.defaultCodecs().maxInMemorySize(1024 * 1024); // 1MB
            
            // 기타 필요한 코덱 설정
            configurer.defaultCodecs().enableLoggingRequestDetails(true);
        };
    }
}