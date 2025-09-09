package com.unicorn.phonebill.product.service;

import com.unicorn.phonebill.product.config.KosProperties;
import com.unicorn.phonebill.product.exception.CircuitBreakerException;
import com.unicorn.phonebill.product.exception.KosConnectionException;
import com.unicorn.phonebill.product.dto.kos.KosCommonResponse;
import com.unicorn.phonebill.product.dto.kos.KosProductInfo;
import com.unicorn.phonebill.product.dto.kos.KosProductListResponse;
import com.unicorn.phonebill.product.dto.kos.KosProductInquiryRequest;
import com.unicorn.phonebill.product.dto.kos.KosProductInquiryResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * KOS 시스템 연동 클라이언트 서비스 (상품 관리)
 * 
 * 통신사 백엔드 시스템(KOS)과의 상품 관련 연동을 담당하는 서비스
 * - Circuit Breaker 패턴으로 외부 시스템 장애 격리
 * - Retry 패턴으로 일시적 네트워크 오류 극복
 * - Timeout 설정으로 응답 지연 방지
 * - 데이터 변환 및 오류 처리
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KosClientService {

    private final RestTemplate restTemplate;
    private final KosProperties kosProperties;

    /**
     * KOS 시스템에서 전체 상품 목록 조회
     * 
     * @return KOS 상품 목록 응답
     */
    @CircuitBreaker(name = "kos-product-list", fallbackMethod = "getProductListFallback")
    @Retry(name = "kos-product-list")
    public List<KosProductInfo> getProductListFromKos() {
        log.info("KOS 상품 목록 조회 요청");

        try {
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Service-Name", "MVNO-PRODUCT-SERVICE");
            headers.set("X-Request-ID", java.util.UUID.randomUUID().toString());

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            // KOS Mock API 호출
            String kosUrl = kosProperties.getProductListUrl();
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    kosUrl, HttpMethod.GET, requestEntity, 
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> response = responseEntity.getBody();
            
            if (response == null) {
                throw KosConnectionException.apiError("KOS-PRODUCT-LIST", 
                        String.valueOf(responseEntity.getStatusCode().value()), "응답 데이터가 없습니다");
            }

            // KosCommonResponse의 data 부분에서 KosProductListResponse 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                throw KosConnectionException.apiError("KOS-PRODUCT-LIST", 
                        "NO_DATA", "응답에서 data를 찾을 수 없습니다");
            }

            // 상품 목록 추출 및 변환
            List<KosProductInfo> productList = convertToKosProductInfoList(data);
            
            log.info("KOS 상품 목록 조회 성공 - 상품 개수: {}", productList.size());
            return productList;

        } catch (HttpClientErrorException e) {
            log.error("KOS API 클라이언트 오류 - 상태: {}, 응답: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw KosConnectionException.apiError("KOS-PRODUCT-LIST", 
                    String.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            log.error("KOS API 서버 오류 - 상태: {}, 응답: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw KosConnectionException.apiError("KOS-PRODUCT-LIST", 
                    String.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());

        } catch (ResourceAccessException e) {
            log.error("KOS 네트워크 연결 오류 - 오류: {}", e.getMessage());
            throw KosConnectionException.networkError("KOS-PRODUCT-LIST", e);

        } catch (Exception e) {
            log.error("KOS 연동 중 예상치 못한 오류 - 오류: {}", e.getMessage(), e);
            throw new KosConnectionException("KOS-PRODUCT-LIST", 
                    "KOS 시스템 연동 중 오류가 발생했습니다", "KOS-PRODUCT-LIST", e);
        }
    }

    /**
     * KOS 시스템에서 상품 변경 처리
     * 
     * @param lineNumber 회선번호
     * @param currentProductCode 현재 상품코드
     * @param targetProductCode 변경할 상품코드
     * @return 상품변경 결과
     */
    @CircuitBreaker(name = "kos-product-change", fallbackMethod = "changeProductFallback")
    @Retry(name = "kos-product-change")
    public Map<String, Object> changeProductInKos(String lineNumber, String currentProductCode, String targetProductCode) {
        log.info("KOS 상품 변경 요청 - 회선: {}, 현재상품: {}, 변경상품: {}", 
                lineNumber, currentProductCode, targetProductCode);

        try {
            // 요청 데이터 구성
            Map<String, Object> requestData = Map.of(
                "lineNumber", lineNumber.replaceAll("-", ""),
                "currentProductCode", currentProductCode,
                "targetProductCode", targetProductCode,
                "requestId", generateRequestId()
            );

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Service-Name", "MVNO-PRODUCT-SERVICE");
            headers.set("X-Request-ID", java.util.UUID.randomUUID().toString());

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestData, headers);

            // KOS Mock API 호출
            String kosUrl = kosProperties.getProductChangeUrl();
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    kosUrl, HttpMethod.POST, requestEntity, 
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> response = responseEntity.getBody();
            
            if (response == null) {
                throw KosConnectionException.apiError("KOS-PRODUCT-CHANGE", 
                        String.valueOf(responseEntity.getStatusCode().value()), "응답 데이터가 없습니다");
            }

            log.info("KOS 상품 변경 성공 - 회선: {}", lineNumber);
            return response;

        } catch (Exception e) {
            log.error("KOS 상품 변경 오류 - 회선: {}, 오류: {}", lineNumber, e.getMessage(), e);
            throw new KosConnectionException("KOS-PRODUCT-CHANGE", 
                    "KOS 시스템 상품 변경 중 오류가 발생했습니다", "KOS-PRODUCT-CHANGE", e);
        }
    }

    /**
     * 상품 목록 조회 Circuit Breaker Fallback 메소드
     */
    public List<KosProductInfo> getProductListFallback(Exception ex) {
        log.warn("KOS 상품 목록 조회 Circuit Breaker 작동 - 오류: {}", ex.getMessage());

        // Circuit Breaker가 Open 상태인 경우
        if (ex.getClass().getSimpleName().contains("CircuitBreakerOpenException")) {
            throw CircuitBreakerException.circuitBreakerOpen("KOS-PRODUCT-LIST");
        }

        // 기본 상품 목록 반환 (빈 목록)
        log.info("KOS 상품 목록 조회 fallback - 빈 목록 반환");
        return new ArrayList<>();
    }

    /**
     * 상품 변경 Circuit Breaker Fallback 메소드
     */
    public Map<String, Object> changeProductFallback(String lineNumber, String currentProductCode, 
                                                    String targetProductCode, Exception ex) {
        log.warn("KOS 상품 변경 Circuit Breaker 작동 - 회선: {}, 오류: {}", lineNumber, ex.getMessage());

        // Circuit Breaker가 Open 상태인 경우
        if (ex.getClass().getSimpleName().contains("CircuitBreakerOpenException")) {
            throw CircuitBreakerException.circuitBreakerOpen("KOS-PRODUCT-CHANGE");
        }

        // 실패 응답 반환
        return Map.of(
            "success", false,
            "resultCode", "9999",
            "resultMessage", "시스템 오류로 인한 상품 변경 실패",
            "timestamp", LocalDateTime.now().toString()
        );
    }

    /**
     * KOS 가입상품 조회
     * 
     * @param lineNumber 회선번호
     * @return KOS 가입상품 조회 응답
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @CircuitBreaker(name = "kosClient", fallbackMethod = "getProductInquiryFallback")
    @Retry(name = "kosClient")
    public KosCommonResponse<KosProductInquiryResponse> getProductInquiry(String lineNumber) {
        log.info("KOS 가입상품 조회 요청: lineNumber={}", lineNumber);
        
        try {
            // 요청 ID 생성
            String requestId = generateRequestId();
            
            // 요청 데이터 생성
            KosProductInquiryRequest request = new KosProductInquiryRequest();
            request.setLineNumber(lineNumber);
            request.setRequestId(requestId);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Request-ID", requestId);
            headers.set("X-Service-Name", "product-service");
            
            HttpEntity<KosProductInquiryRequest> requestEntity = new HttpEntity<>(request, headers);
            
            // KOS API 호출
            String url = kosProperties.getProductInquiryUrl();
            log.debug("KOS API 호출: url={}, requestId={}", url, requestId);
            
            ResponseEntity<KosCommonResponse<KosProductInquiryResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<KosCommonResponse<KosProductInquiryResponse>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                KosCommonResponse<KosProductInquiryResponse> kosResponse = response.getBody();
                
                if (kosResponse.getSuccess()) {
                    log.info("KOS 가입상품 조회 성공: lineNumber={}, requestId={}", lineNumber, requestId);
                    return kosResponse;
                } else {
                    log.error("KOS 가입상품 조회 실패: lineNumber={}, requestId={}, resultCode={}, resultMessage={}", 
                                lineNumber, requestId, kosResponse.getResultCode(), kosResponse.getResultMessage());
                    throw new RuntimeException("KOS 가입상품 조회 실패: " + kosResponse.getResultMessage());
                }
            } else {
                throw new RuntimeException("KOS API 호출 실패: HTTP " + response.getStatusCode());
            }
            
        } catch (ResourceAccessException e) {
            log.error("KOS API 호출 중 네트워크 오류: lineNumber={}", lineNumber, e);
            throw new RuntimeException("KOS 시스템과의 통신 중 오류가 발생했습니다", e);
        } catch (Exception e) {
            log.error("KOS 가입상품 조회 중 예상치 못한 오류: lineNumber={}", lineNumber, e);
            throw new RuntimeException("가입상품 조회 중 시스템 오류가 발생했습니다", e);
        }
    }

    /**
     * KOS 가입상품 조회 실패 시 Fallback 메서드
     */
    public KosCommonResponse<KosProductInquiryResponse> getProductInquiryFallback(String lineNumber, Exception ex) {
        log.error("KOS 가입상품 조회 Fallback 실행: lineNumber={}, reason={}", lineNumber, ex.getMessage());
        
        // Fallback 응답 생성 (임시 데이터)
        KosProductInquiryResponse.ProductInfo productInfo = KosProductInquiryResponse.ProductInfo.builder()
                .lineNumber(lineNumber)
                .currentProductCode("FALLBACK_PLAN")
                .currentProductName("임시 플랜 (시스템 점검 중)")
                .monthlyFee(new java.math.BigDecimal("0"))
                .dataAllowance("정보 없음")
                .voiceAllowance("정보 없음")
                .smsAllowance("정보 없음")
                .productStatus("UNKNOWN")
                .contractDate(java.time.LocalDateTime.now())
                .build();

        KosProductInquiryResponse.CustomerInfo customerInfo = KosProductInquiryResponse.CustomerInfo.builder()
                .customerName("임시 고객")
                .customerId("FALLBACK_CUSTOMER")
                .operatorCode("UNKNOWN")
                .lineStatus("UNKNOWN")
                .build();

        KosProductInquiryResponse data = KosProductInquiryResponse.builder()
                .requestId(generateRequestId())
                .procStatus("FALLBACK")
                .resultCode("9999")
                .resultMessage("KOS 시스템 일시 점검 중입니다")
                .productInfo(productInfo)
                .customerInfo(customerInfo)
                .build();

        return KosCommonResponse.success(data, "KOS 시스템 일시 점검 중 - 임시 정보 제공");
    }

    /**
     * KOS 시스템 연결 상태 확인
     * 
     * @return 연결 가능 여부
     */
    @CircuitBreaker(name = "kos-health-check")
    public boolean isKosSystemAvailable() {
        try {
            String healthUrl = kosProperties.getHealthCheckUrl();
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            
            boolean available = response.getStatusCode().is2xxSuccessful();
            log.debug("KOS 시스템 상태 확인 - 사용가능: {}", available);
            
            return available;
        } catch (Exception e) {
            log.warn("KOS 시스템 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * Map 데이터를 KosProductInfo 리스트로 변환
     */
    private List<KosProductInfo> convertToKosProductInfoList(Map<String, Object> data) {
        try {
            List<KosProductInfo> productList = new ArrayList<>();
            
            // products 배열 추출
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> products = (List<Map<String, Object>>) data.get("products");
            if (products != null) {
                for (Map<String, Object> product : products) {
                    KosProductInfo productInfo = KosProductInfo.builder()
                            .productCode((String) product.get("product_code"))
                            .productName((String) product.get("product_name"))
                            .productType((String) product.get("product_type"))
                            .monthlyFee(convertToInteger(product.get("monthly_fee")))
                            .dataAllowance(convertToInteger(product.get("data_allowance")))
                            .voiceAllowance(convertToInteger(product.get("voice_allowance")))
                            .smsAllowance(convertToInteger(product.get("sms_allowance")))
                            .networkType((String) product.get("network_type"))
                            .status((String) product.get("status"))
                            .description((String) product.get("description"))
                            .build();
                    
                    productList.add(productInfo);
                }
            }
            
            return productList;
            
        } catch (Exception e) {
            log.error("상품 목록 데이터 변환 오류: {}", e.getMessage(), e);
            throw KosConnectionException.dataConversionError("KOS-PRODUCT-LIST", "KosProductInfo", e);
        }
    }

    /**
     * Object를 Integer로 변환
     */
    private Integer convertToInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.valueOf(value.toString().split("\\.")[0]); // 소수점 제거
        } catch (NumberFormatException e) {
            log.warn("숫자 변환 실패: {}", value);
            return 0;
        }
    }

    /**
     * 요청 ID 생성
     */
    private String generateRequestId() {
        String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("PROD_%s_%s", currentDate, uuid);
    }
}