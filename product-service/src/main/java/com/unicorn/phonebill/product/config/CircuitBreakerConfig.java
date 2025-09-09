package com.unicorn.phonebill.product.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker 패턴 설정
 * 
 * Resilience4j를 활용한 장애 격리 및 복구 시스템 구성
 * - KOS 시스템 연동 시 장애 상황에 대한 자동 회복
 * - 실패율 기반 Circuit Breaker
 * - 응답 시간 기반 Time Limiter
 * - 재시도 정책 구성
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-09
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CircuitBreakerConfig {

    private final KosProperties kosProperties;

    /**
     * KOS 시스템 연동용 Circuit Breaker 구성
     * 
     * @return Circuit Breaker 레지스트리
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        log.info("Circuit Breaker 레지스트리 구성 시작");

        // KOS 시스템용 Circuit Breaker 설정
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig kosCircuitBreakerConfig = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                // 실패율 임계값 (50%)
                .failureRateThreshold(kosProperties.getCircuitBreaker().getFailureRateThreshold() * 100)
                // 느린 호출 임계값 (10초)
                .slowCallDurationThreshold(Duration.ofMillis(
                    kosProperties.getCircuitBreaker().getSlowCallDurationThreshold()))
                // 느린 호출 비율 임계값 (50%)
                .slowCallRateThreshold(kosProperties.getCircuitBreaker().getSlowCallRateThreshold() * 100)
                // 슬라이딩 윈도우 크기 (10회)
                .slidingWindowSize(kosProperties.getCircuitBreaker().getSlidingWindowSize())
                // 슬라이딩 윈도우 타입 (횟수 기반)
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                // 최소 호출 수 (5회)
                .minimumNumberOfCalls(kosProperties.getCircuitBreaker().getMinimumNumberOfCalls())
                // Half-Open 상태에서 허용되는 호출 수 (3회)
                .permittedNumberOfCallsInHalfOpenState(
                    kosProperties.getCircuitBreaker().getPermittedNumberOfCallsInHalfOpenState())
                // Open 상태 유지 시간 (60초)
                .waitDurationInOpenState(Duration.ofMillis(
                    kosProperties.getCircuitBreaker().getWaitDurationInOpenState()))
                // Circuit Breaker 상태 변경 이벤트 리스너
                .recordExceptions(Exception.class)
                .ignoreExceptions()
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(kosCircuitBreakerConfig);

        // KOS Circuit Breaker 등록
        CircuitBreaker kosCircuitBreaker = registry.circuitBreaker("kos-system", kosCircuitBreakerConfig);

        // 이벤트 리스너 등록
        kosCircuitBreaker.getEventPublisher()
            .onStateTransition(event -> {
                log.warn("Circuit Breaker 상태 변경 - From: {}, To: {}", 
                    event.getStateTransition().getFromState(), 
                    event.getStateTransition().getToState());
            })
            .onCallNotPermitted(event -> {
                log.error("Circuit Breaker OPEN 상태 - 호출 차단됨: {}", event.getCircuitBreakerName());
            })
            .onFailureRateExceeded(event -> {
                log.error("Circuit Breaker 실패율 초과");
            });

        log.info("Circuit Breaker 레지스트리 구성 완료 - KOS Circuit Breaker 등록됨");
        return registry;
    }

    /**
     * 재시도 정책 레지스트리 구성
     * 
     * @return 재시도 레지스트리
     */
    @Bean
    public RetryRegistry retryRegistry() {
        log.info("Retry 레지스트리 구성 시작");

        // KOS 시스템용 재시도 설정
        io.github.resilience4j.retry.RetryConfig kosRetryConfig = 
            io.github.resilience4j.retry.RetryConfig.custom()
                // 최대 재시도 횟수
                .maxAttempts(kosProperties.getMaxRetries())
                // 재시도 간격
                .waitDuration(Duration.ofMillis(kosProperties.getRetryDelay()))
                // 지수 백오프 비활성화 (고정 간격 사용)
                // .intervalFunction() 대신 waitDuration 사용
                // 재시도 대상 예외
                .retryExceptions(Exception.class)
                // 재시도 제외 예외
                .ignoreExceptions(IllegalArgumentException.class, SecurityException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(kosRetryConfig);

        // KOS Retry 등록
        Retry kosRetry = registry.retry("kos-system", kosRetryConfig);

        // 재시도 이벤트 리스너
        kosRetry.getEventPublisher()
            .onRetry(event -> {
                log.warn("재시도 실행 - 시도 횟수: {}/{}, 마지막 오류: {}", 
                    event.getNumberOfRetryAttempts(), 
                    kosRetryConfig.getMaxAttempts(),
                    event.getLastThrowable().getMessage());
            })
            .onError(event -> {
                log.error("재시도 최종 실패 - 총 시도 횟수: {}, 최종 오류: {}", 
                    event.getNumberOfRetryAttempts(), 
                    event.getLastThrowable().getMessage());
            });

        log.info("Retry 레지스트리 구성 완료 - 최대 재시도: {}회, 간격: {}ms", 
            kosProperties.getMaxRetries(), kosProperties.getRetryDelay());
        return registry;
    }

    /**
     * Time Limiter 레지스트리 구성
     * 
     * @return Time Limiter 레지스트리
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        log.info("Time Limiter 레지스트리 구성 시작");

        // KOS 시스템용 타임아웃 설정
        io.github.resilience4j.timelimiter.TimeLimiterConfig kosTimeLimiterConfig = 
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                // 타임아웃 (연결 타임아웃 + 읽기 타임아웃)
                .timeoutDuration(Duration.ofMillis(kosProperties.getTotalTimeout()))
                // 타임아웃 시 작업 취소 여부
                .cancelRunningFuture(true)
                .build();

        TimeLimiterRegistry registry = TimeLimiterRegistry.of(kosTimeLimiterConfig);

        // KOS Time Limiter 등록
        TimeLimiter kosTimeLimiter = registry.timeLimiter("kos-system", kosTimeLimiterConfig);

        // 타임아웃 이벤트 리스너
        kosTimeLimiter.getEventPublisher()
            .onTimeout(event -> {
                log.error("Time Limiter 타임아웃 발생 - 설정 시간: {}ms", 
                    kosTimeLimiterConfig.getTimeoutDuration().toMillis());
            });

        log.info("Time Limiter 레지스트리 구성 완료 - 타임아웃: {}ms", 
            kosProperties.getTotalTimeout());
        return registry;
    }

    /**
     * KOS Circuit Breaker 조회
     * 
     * @param circuitBreakerRegistry Circuit Breaker 레지스트리
     * @return KOS Circuit Breaker
     */
    @Bean
    public CircuitBreaker kosCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker("kos-system");
    }

    /**
     * KOS Retry 조회
     * 
     * @param retryRegistry Retry 레지스트리
     * @return KOS Retry
     */
    @Bean
    public Retry kosRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("kos-system");
    }

    /**
     * KOS Time Limiter 조회
     * 
     * @param timeLimiterRegistry Time Limiter 레지스트리
     * @return KOS Time Limiter
     */
    @Bean
    public TimeLimiter kosTimeLimiter(TimeLimiterRegistry timeLimiterRegistry) {
        return timeLimiterRegistry.timeLimiter("kos-system");
    }
}