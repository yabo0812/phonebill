package com.phonebill.bill.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * KOS 시스템 연동 설정 프로퍼티
 * 
 * application.yml 파일의 kos 설정을 바인딩하는 설정 클래스
 * - 연결 정보 (URL, 타임아웃 등)
 * - 재시도 정책
 * - Circuit Breaker 설정
 * - 인증 관련 설정
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Component
@ConfigurationProperties(prefix = "kos")
@Getter
@Setter
@Validated
public class KosProperties {

    /**
     * KOS 시스템 기본 URL
     */
    @NotBlank(message = "KOS 기본 URL은 필수입니다")
    private String baseUrl;

    /**
     * 연결 타임아웃 (밀리초)
     */
    @NotNull
    @Positive
    private Integer connectTimeout = 5000;

    /**
     * 읽기 타임아웃 (밀리초)
     */
    @NotNull
    @Positive
    private Integer readTimeout = 30000;

    /**
     * 최대 재시도 횟수
     */
    @NotNull
    @Positive
    private Integer maxRetries = 3;

    /**
     * 재시도 간격 (밀리초)
     */
    @NotNull
    @Positive
    private Long retryDelay = 1000L;

    /**
     * Circuit Breaker 설정
     */
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    /**
     * 인증 설정
     */
    private Authentication authentication = new Authentication();

    /**
     * 모니터링 설정
     */
    private Monitoring monitoring = new Monitoring();

    /**
     * Circuit Breaker 설정 내부 클래스
     */
    @Getter
    @Setter
    public static class CircuitBreaker {

        /**
         * 실패율 임계값 (0.0 ~ 1.0)
         */
        private Float failureRateThreshold = 0.5f;

        /**
         * 느린 호출 임계값 (밀리초)
         */
        private Long slowCallDurationThreshold = 10000L;

        /**
         * 느린 호출 비율 임계값 (0.0 ~ 1.0)
         */
        private Float slowCallRateThreshold = 0.5f;

        /**
         * 슬라이딩 윈도우 크기
         */
        private Integer slidingWindowSize = 10;

        /**
         * 최소 호출 수
         */
        private Integer minimumNumberOfCalls = 5;

        /**
         * Half-Open 상태에서 허용되는 호출 수
         */
        private Integer permittedNumberOfCallsInHalfOpenState = 3;

        /**
         * Open 상태 유지 시간 (밀리초)
         */
        private Long waitDurationInOpenState = 60000L;
    }

    /**
     * 인증 설정 내부 클래스
     */
    @Getter
    @Setter
    public static class Authentication {

        /**
         * 인증 토큰 사용 여부
         */
        private Boolean enabled = true;

        /**
         * API 키
         */
        private String apiKey;

        /**
         * 시크릿 키
         */
        private String secretKey;

        /**
         * 토큰 만료 시간 (초)
         */
        private Long tokenExpirationSeconds = 3600L;

        /**
         * 토큰 갱신 임계 시간 (초)
         */
        private Long tokenRefreshThresholdSeconds = 300L;
    }

    /**
     * 모니터링 설정 내부 클래스
     */
    @Getter
    @Setter
    public static class Monitoring {

        /**
         * 성능 로깅 사용 여부
         */
        private Boolean performanceLoggingEnabled = true;

        /**
         * 느린 요청 임계값 (밀리초)
         */
        private Long slowRequestThreshold = 3000L;

        /**
         * 메트릭 수집 사용 여부
         */
        private Boolean metricsEnabled = true;

        /**
         * 상태 체크 주기 (밀리초)
         */
        private Long healthCheckInterval = 30000L;
    }

    // === Computed Properties ===

    /**
     * 요금조회 API URL 조회
     * 
     * @return 요금조회 API 전체 URL
     */
    public String getBillInquiryUrl() {
        return baseUrl + "/api/bill/inquiry";
    }

    /**
     * 상태 확인 API URL 조회
     * 
     * @return 상태 확인 API 전체 URL
     */
    public String getStatusCheckUrl() {
        return baseUrl + "/api/bill/status";
    }

    /**
     * 헬스체크 API URL 조회
     * 
     * @return 헬스체크 API 전체 URL
     */
    public String getHealthCheckUrl() {
        return baseUrl + "/health";
    }

    /**
     * 전체 타임아웃 계산 (연결 + 읽기)
     * 
     * @return 전체 타임아웃 (밀리초)
     */
    public Integer getTotalTimeout() {
        return connectTimeout + readTimeout;
    }

    /**
     * 최대 재시도 시간 계산
     * 
     * @return 최대 재시도 시간 (밀리초)
     */
    public Long getMaxRetryDuration() {
        return retryDelay * maxRetries;
    }

    // === Validation Methods ===

    /**
     * 설정 유효성 검증
     * 
     * @return 유효한 설정인지 여부
     */
    public boolean isValid() {
        return baseUrl != null && !baseUrl.trim().isEmpty() &&
               connectTimeout > 0 && readTimeout > 0 &&
               maxRetries > 0 && retryDelay > 0;
    }

    /**
     * Circuit Breaker 설정 유효성 검증
     * 
     * @return 유효한 설정인지 여부
     */
    public boolean isCircuitBreakerConfigValid() {
        return circuitBreaker.failureRateThreshold >= 0.0f && circuitBreaker.failureRateThreshold <= 1.0f &&
               circuitBreaker.slowCallRateThreshold >= 0.0f && circuitBreaker.slowCallRateThreshold <= 1.0f &&
               circuitBreaker.slidingWindowSize > 0 &&
               circuitBreaker.minimumNumberOfCalls > 0 &&
               circuitBreaker.permittedNumberOfCallsInHalfOpenState > 0;
    }

    /**
     * 인증 설정 유효성 검증
     * 
     * @return 유효한 설정인지 여부
     */
    public boolean isAuthenticationConfigValid() {
        if (!authentication.enabled) {
            return true;
        }
        return authentication.apiKey != null && !authentication.apiKey.trim().isEmpty() &&
               authentication.secretKey != null && !authentication.secretKey.trim().isEmpty();
    }

    // === Utility Methods ===

    /**
     * 설정 정보 요약
     * 
     * @return 설정 요약 문자열
     */
    public String getConfigSummary() {
        return String.format(
            "KOS 설정 - URL: %s, 연결타임아웃: %dms, 읽기타임아웃: %dms, 재시도: %d회",
            baseUrl, connectTimeout, readTimeout, maxRetries
        );
    }

    /**
     * 마스킹된 인증 정보 조회 (로깅용)
     * 
     * @return 마스킹된 인증 정보
     */
    public String getMaskedAuthInfo() {
        if (!authentication.enabled || authentication.apiKey == null) {
            return "인증 비활성화";
        }
        
        String maskedApiKey = authentication.apiKey.length() > 8 ?
            authentication.apiKey.substring(0, 4) + "****" + 
            authentication.apiKey.substring(authentication.apiKey.length() - 4) :
            "****";
        
        return String.format("API키: %s, 토큰만료: %d초", maskedApiKey, authentication.tokenExpirationSeconds);
    }
}