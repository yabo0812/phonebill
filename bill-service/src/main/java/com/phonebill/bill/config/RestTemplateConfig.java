package com.phonebill.bill.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 설정
 * 
 * KOS 시스템 및 외부 API 연동을 위한 HTTP 클라이언트 설정
 * - 연결 타임아웃 설정
 * - 읽기 타임아웃 설정
 * - 요청/응답 로깅 인터셉터
 * - 에러 핸들러 설정
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final KosProperties kosProperties;

    /**
     * KOS 시스템 연동용 RestTemplate 구성
     * 
     * @param restTemplateBuilder RestTemplate 빌더
     * @return KOS용 RestTemplate
     */
    @Bean
    public RestTemplate kosRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        log.info("KOS RestTemplate 구성 시작");

        RestTemplate restTemplate = restTemplateBuilder
                // 타임아웃 설정
                .setConnectTimeout(Duration.ofMillis(kosProperties.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(kosProperties.getReadTimeout()))
                
                // 요청 팩토리 설정
                .requestFactory(() -> createRequestFactory())
                
                // 기본 에러 핸들러 설정
                .errorHandler(new RestTemplateErrorHandler())
                
                // 요청/응답 로깅 인터셉터 추가
                .additionalInterceptors(new RestTemplateLoggingInterceptor())
                
                .build();

        log.info("KOS RestTemplate 구성 완료 - 연결타임아웃: {}ms, 읽기타임아웃: {}ms", 
            kosProperties.getConnectTimeout(), kosProperties.getReadTimeout());

        return restTemplate;
    }

    /**
     * 일반용 RestTemplate 구성
     * 
     * @param restTemplateBuilder RestTemplate 빌더
     * @return 일반용 RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        log.info("일반 RestTemplate 구성 시작");

        RestTemplate restTemplate = restTemplateBuilder
                // 기본 타임아웃 설정 (더 관대한 설정)
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                
                // 요청 팩토리 설정
                .requestFactory(() -> createRequestFactory())
                
                // 기본 에러 핸들러 설정
                .errorHandler(new RestTemplateErrorHandler())
                
                .build();

        log.info("일반 RestTemplate 구성 완료");
        return restTemplate;
    }

    /**
     * HTTP 요청 팩토리 생성
     * 
     * @return 클라이언트 HTTP 요청 팩토리
     */
    private ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // 연결 타임아웃 설정
        factory.setConnectTimeout(kosProperties.getConnectTimeout());
        
        // 읽기 타임아웃 설정
        factory.setReadTimeout(kosProperties.getReadTimeout());
        
        // 요청/응답 본문을 여러 번 읽을 수 있도록 버퍼링 활성화
        return new BufferingClientHttpRequestFactory(factory);
    }

    /**
     * RestTemplate 로깅 인터셉터
     * 
     * 요청 및 응답 로그를 기록하는 인터셉터
     */
    private class RestTemplateLoggingInterceptor implements 
            org.springframework.http.client.ClientHttpRequestInterceptor {
        
        @Override
        public org.springframework.http.client.ClientHttpResponse intercept(
                org.springframework.http.HttpRequest request,
                byte[] body,
                org.springframework.http.client.ClientHttpRequestExecution execution) throws java.io.IOException {
            
            long startTime = System.currentTimeMillis();
            
            // 요청 로깅
            if (log.isDebugEnabled()) {
                log.debug("HTTP 요청 - 메소드: {}, URI: {}, 헤더: {}", 
                    request.getMethod(), request.getURI(), request.getHeaders());
                
                if (body.length > 0) {
                    log.debug("HTTP 요청 본문: {}", new String(body, java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            
            // 요청 실행
            org.springframework.http.client.ClientHttpResponse response = execution.execute(request, body);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // 응답 로깅
            if (log.isDebugEnabled()) {
                log.debug("HTTP 응답 - 상태: {}, 소요시간: {}ms, 헤더: {}", 
                    response.getStatusCode(), duration, response.getHeaders());
                
                try {
                    String responseBody = new String(
                        response.getBody().readAllBytes(), 
                        java.nio.charset.StandardCharsets.UTF_8
                    );
                    log.debug("HTTP 응답 본문: {}", responseBody);
                } catch (Exception e) {
                    log.debug("HTTP 응답 본문 읽기 실패: {}", e.getMessage());
                }
            }
            
            // 성능 모니터링 로그
            if (duration > kosProperties.getMonitoring().getSlowRequestThreshold()) {
                log.warn("느린 HTTP 요청 감지 - URI: {}, 소요시간: {}ms, 임계값: {}ms", 
                    request.getURI(), duration, kosProperties.getMonitoring().getSlowRequestThreshold());
            }
            
            return response;
        }
    }

    /**
     * RestTemplate 에러 핸들러
     * 
     * HTTP 에러 응답을 커스텀 예외로 변환하는 핸들러
     */
    private static class RestTemplateErrorHandler implements org.springframework.web.client.ResponseErrorHandler {
        
        @Override
        public boolean hasError(org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
            return response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError();
        }
        
        @Override
        public void handleError(org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
            String statusCode = response.getStatusCode().toString();
            String statusText = response.getStatusText();
            
            String responseBody = "";
            try {
                responseBody = new String(
                    response.getBody().readAllBytes(), 
                    java.nio.charset.StandardCharsets.UTF_8
                );
            } catch (Exception e) {
                log.debug("HTTP 에러 응답 본문 읽기 실패: {}", e.getMessage());
            }
            
            log.error("HTTP 에러 응답 - 상태: {} {}, 응답 본문: {}", 
                statusCode, statusText, responseBody);
            
            // 상태 코드별 예외 처리
            if (response.getStatusCode().is4xxClientError()) {
                throw new RuntimeException(
                    String.format("클라이언트 오류 - %s %s: %s", statusCode, statusText, responseBody)
                );
            } else if (response.getStatusCode().is5xxServerError()) {
                throw new RuntimeException(
                    String.format("서버 오류 - %s %s: %s", statusCode, statusText, responseBody)
                );
            }
        }
    }
}