package com.phonebill.bill.service;

import com.phonebill.bill.config.KosProperties;
import com.phonebill.bill.dto.BillInquiryResponse;
import com.phonebill.bill.exception.CircuitBreakerException;
import com.phonebill.bill.exception.KosConnectionException;
import com.phonebill.bill.external.KosRequest;
import com.phonebill.bill.external.KosResponse;
import com.phonebill.kosmock.dto.KosCommonResponse;
import com.phonebill.kosmock.dto.KosBillInquiryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.CompletableFuture;

/**
 * KOS 시스템 연동 클라이언트 서비스
 * 
 * 통신사 백엔드 시스템(KOS)과의 연동을 담당하는 서비스
 * - Circuit Breaker 패턴으로 외부 시스템 장애 격리
 * - Retry 패턴으로 일시적 네트워크 오류 극복
 * - Timeout 설정으로 응답 지연 방지
 * - 데이터 변환 및 오류 처리
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KosClientService {

    private final RestTemplate restTemplate;
    private final KosProperties kosProperties;

    /**
     * KOS Mock 시스템에서 요금 정보 조회 (KosBillInquiryResponse 직접 반환)
     * 
     * @param lineNumber 회선번호
     * @param inquiryMonth 조회월
     * @return KOS 원본 응답 데이터
     */
    @CircuitBreaker(name = "kos-bill-inquiry-direct", fallbackMethod = "inquireBillDirectFallback")
    @Retry(name = "kos-bill-inquiry-direct")
    public KosBillInquiryResponse inquireBillFromKosDirect(String lineNumber, String inquiryMonth) {
        log.info("KOS Mock 직접 호출 - 회선: {}, 조회월: {}", lineNumber, inquiryMonth);

        try {
            // 회선번호 형식 변환 (010-1234-5678 → 01012345678)
            String formattedLineNumber = lineNumber.replaceAll("-", "");
            
            // KOS Mock 요청 데이터 구성 (KosBillInquiryRequest 형식)
            Map<String, Object> kosRequest = Map.of(
                "lineNumber", formattedLineNumber,
                "billingMonth", inquiryMonth,
                "requestId", generateRequestId()
            );
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Service-Name", "MVNO-BILL-INQUIRY");
            headers.set("X-Request-ID", java.util.UUID.randomUUID().toString());

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(kosRequest, headers);

            // KOS Mock API 호출
            String kosUrl = kosProperties.getBaseUrl() + "/api/v1/kos/bill/inquiry";
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    kosUrl, HttpMethod.POST, requestEntity, Map.class
            );

            Map<String, Object> response = responseEntity.getBody();
            
            if (response == null) {
                throw KosConnectionException.apiError("KOS-BILL-INQUIRY", 
                        String.valueOf(responseEntity.getStatusCodeValue()), "응답 데이터가 없습니다");
            }

            // KosCommonResponse의 data 부분에서 KosBillInquiryResponse 추출
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                throw KosConnectionException.apiError("KOS-BILL-INQUIRY", 
                        "NO_DATA", "응답에서 data를 찾을 수 없습니다");
            }

            // KosBillInquiryResponse 객체 구성
            KosBillInquiryResponse result = convertMapToKosBillInquiryResponse(data);
            
            log.info("KOS Mock 직접 호출 성공 - 요청ID: {}", result.getRequestId());
            return result;

        } catch (Exception e) {
            log.error("KOS Mock 직접 호출 오류 - 회선: {}, 오류: {}", lineNumber, e.getMessage(), e);
            throw new KosConnectionException("KOS-BILL-INQUIRY-DIRECT", 
                    "KOS Mock 시스템 연동 중 오류가 발생했습니다", e);
        }
    }

    /**
     * KOS 시스템에서 요금 정보 조회 (동기 처리)
     * 
     * Circuit Breaker, Retry 패턴 적용
     * 
     * @param lineNumber 회선번호
     * @param inquiryMonth 조회월
     * @return 요금조회 응답
     */
    @CircuitBreaker(name = "kos-bill-inquiry", fallbackMethod = "inquireBillSyncFallback")
    @Retry(name = "kos-bill-inquiry")
    public BillInquiryResponse inquireBillFromKosSync(String lineNumber, String inquiryMonth) {
        log.info("KOS 요금조회 요청 (동기) - 회선: {}, 조회월: {}", lineNumber, inquiryMonth);

        try {
            // KOS 요청 데이터 구성
            KosRequest kosRequest = KosRequest.createBillInquiryRequest(lineNumber, inquiryMonth, "system");
            
            log.info("KOS Mock으로 전송하는 요청: lineNumber={}, inquiryMonth={}, requestId={}", 
                    kosRequest.getLineNumber(), kosRequest.getInquiryMonth(), kosRequest.getRequestId());

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Service-Name", "MVNO-BILL-INQUIRY");
            headers.set("X-Request-ID", java.util.UUID.randomUUID().toString());

            HttpEntity<KosRequest> requestEntity = new HttpEntity<>(kosRequest, headers);

            // KOS API 호출 (KOS Mock 응답 구조에 맞게 수정)
            String kosUrl = kosProperties.getBaseUrl() + "/api/v1/kos/bill/inquiry";
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    kosUrl, HttpMethod.POST, requestEntity, Map.class
            );

            Map<String, Object> kosCommonResponse = responseEntity.getBody();
            
            log.info("KOS Mock 응답 받음: {}", kosCommonResponse);
            
            if (kosCommonResponse == null) {
                throw KosConnectionException.apiError("KOS-BILL-INQUIRY", 
                        String.valueOf(responseEntity.getStatusCodeValue()), "응답 데이터가 없습니다");
            }

            // KOS Mock 응답을 내부 모델로 변환
            BillInquiryResponse response = convertKosMockResponseToBillResponse(kosCommonResponse);

            log.info("KOS 요금조회 성공 (동기) - 회선: {}, 조회월: {}, 상태: {}", 
                    lineNumber, inquiryMonth, response.getStatus());
            
            return response;

        } catch (HttpClientErrorException e) {
            log.error("KOS API 클라이언트 오류 - 회선: {}, 상태: {}, 응답: {}", 
                    lineNumber, e.getStatusCode(), e.getResponseBodyAsString());
            throw KosConnectionException.apiError("KOS-BILL-INQUIRY", 
                    String.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            log.error("KOS API 서버 오류 - 회선: {}, 상태: {}, 응답: {}", 
                    lineNumber, e.getStatusCode(), e.getResponseBodyAsString());
            throw KosConnectionException.apiError("KOS-BILL-INQUIRY", 
                    String.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());

        } catch (ResourceAccessException e) {
            log.error("KOS 네트워크 연결 오류 - 회선: {}, 오류: {}", lineNumber, e.getMessage());
            throw KosConnectionException.networkError("KOS-BILL-INQUIRY", e);

        } catch (Exception e) {
            log.error("KOS 연동 중 예상치 못한 오류 - 회선: {}, 오류: {}", lineNumber, e.getMessage(), e);
            throw new KosConnectionException("KOS-BILL-INQUIRY", 
                    "KOS 시스템 연동 중 오류가 발생했습니다", e);
        }
    }

    /**
     * KOS 시스템에서 요금 정보 조회 (비동기 처리)
     * 
     * Circuit Breaker, Retry, TimeLimiter 패턴 적용
     * 
     * @param lineNumber 회선번호
     * @param inquiryMonth 조회월
     * @return 요금조회 응답
     */
    @CircuitBreaker(name = "kos-bill-inquiry", fallbackMethod = "inquireBillFallback")
    @Retry(name = "kos-bill-inquiry")
    @TimeLimiter(name = "kos-bill-inquiry")
    public CompletableFuture<BillInquiryResponse> inquireBillFromKos(String lineNumber, String inquiryMonth) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("KOS 요금조회 요청 - 회선: {}, 조회월: {}", lineNumber, inquiryMonth);

            try {
                // KOS 요청 데이터 구성
                KosRequest kosRequest = KosRequest.createBillInquiryRequest(lineNumber, inquiryMonth, "system");

                // HTTP 헤더 설정
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                headers.set("X-Service-Name", "MVNO-BILL-INQUIRY");
                headers.set("X-Request-ID", java.util.UUID.randomUUID().toString());

                HttpEntity<KosRequest> requestEntity = new HttpEntity<>(kosRequest, headers);

                // KOS API 호출
                String kosUrl = kosProperties.getBaseUrl() + "/api/v1/kos/bill/inquiry";
                ResponseEntity<KosResponse> responseEntity = restTemplate.exchange(
                        kosUrl, HttpMethod.POST, requestEntity, KosResponse.class
                );

                KosResponse kosResponse = responseEntity.getBody();
                
                if (kosResponse == null) {
                    throw KosConnectionException.apiError("KOS-BILL-INQUIRY", 
                            String.valueOf(responseEntity.getStatusCodeValue()), "응답 데이터가 없습니다");
                }

                // KOS 응답을 내부 모델로 변환
                BillInquiryResponse response = convertKosResponseToBillResponse(kosResponse);

                log.info("KOS 요금조회 성공 - 회선: {}, 조회월: {}, 상태: {}", 
                        lineNumber, inquiryMonth, response.getStatus());
                
                return response;

            } catch (HttpClientErrorException e) {
                log.error("KOS API 클라이언트 오류 - 회선: {}, 상태: {}, 응답: {}", 
                        lineNumber, e.getStatusCode(), e.getResponseBodyAsString());
                throw KosConnectionException.apiError("KOS-BILL-INQUIRY", 
                        String.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());

            } catch (HttpServerErrorException e) {
                log.error("KOS API 서버 오류 - 회선: {}, 상태: {}, 응답: {}", 
                        lineNumber, e.getStatusCode(), e.getResponseBodyAsString());
                throw KosConnectionException.apiError("KOS-BILL-INQUIRY", 
                        String.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());

            } catch (ResourceAccessException e) {
                log.error("KOS 네트워크 연결 오류 - 회선: {}, 오류: {}", lineNumber, e.getMessage());
                throw KosConnectionException.networkError("KOS-BILL-INQUIRY", e);

            } catch (Exception e) {
                log.error("KOS 연동 중 예상치 못한 오류 - 회선: {}, 오류: {}", lineNumber, e.getMessage(), e);
                throw new KosConnectionException("KOS-BILL-INQUIRY", 
                        "KOS 시스템 연동 중 오류가 발생했습니다", e);
            }
        });
    }

    /**
     * KOS 요금조회 동기 처리 Circuit Breaker Fallback 메소드
     */
    public BillInquiryResponse inquireBillSyncFallback(String lineNumber, String inquiryMonth, Exception ex) {
        log.warn("KOS 요금조회 동기 처리 Circuit Breaker 작동 - 회선: {}, 조회월: {}, 오류: {}", 
                lineNumber, inquiryMonth, ex.getMessage());

        // Circuit Breaker가 Open 상태인 경우
        if (ex.getClass().getSimpleName().contains("CircuitBreakerOpenException")) {
            throw CircuitBreakerException.circuitBreakerOpen("KOS-BILL-INQUIRY");
        }

        // 기타 오류의 경우 실패 처리로 전환
        BillInquiryResponse fallbackResponse = BillInquiryResponse.builder()
                .status(BillInquiryResponse.ProcessStatus.FAILED)
                .build();

        log.info("KOS 요금조회 동기 처리 fallback 응답 - 실패 처리로 전환");
        return fallbackResponse;
    }

    /**
     * KOS 요금조회 Circuit Breaker Fallback 메소드
     */
    public CompletableFuture<BillInquiryResponse> inquireBillFallback(String lineNumber, String inquiryMonth, Exception ex) {
        log.warn("KOS 요금조회 Circuit Breaker 작동 - 회선: {}, 조회월: {}, 오류: {}", 
                lineNumber, inquiryMonth, ex.getMessage());

        // Circuit Breaker가 Open 상태인 경우
        if (ex.getClass().getSimpleName().contains("CircuitBreakerOpenException")) {
            throw CircuitBreakerException.circuitBreakerOpen("KOS-BILL-INQUIRY");
        }

        // 기타 오류의 경우 비동기 처리로 전환
        BillInquiryResponse fallbackResponse = BillInquiryResponse.builder()
                .status(BillInquiryResponse.ProcessStatus.PROCESSING)
                .build();

        log.info("KOS 요금조회 fallback 응답 - 비동기 처리로 전환");
        return CompletableFuture.completedFuture(fallbackResponse);
    }

    /**
     * KOS 시스템에서 요금조회 상태 확인
     * 
     * @param requestId 요청 ID
     * @return 요금조회 응답
     */
    @CircuitBreaker(name = "kos-status-check", fallbackMethod = "checkInquiryStatusFallback")
    @Retry(name = "kos-status-check")
    public BillInquiryResponse checkInquiryStatus(String requestId) {
        log.info("KOS 요금조회 상태 확인 - 요청ID: {}", requestId);

        try {
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Name", "MVNO-BILL-INQUIRY");
            headers.set("X-Request-ID", requestId);

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            // KOS 상태 확인 API 호출
            String kosUrl = kosProperties.getBaseUrl() + "/api/bill/status/" + requestId;
            ResponseEntity<KosResponse> responseEntity = restTemplate.exchange(
                    kosUrl, HttpMethod.GET, requestEntity, KosResponse.class
            );

            KosResponse kosResponse = responseEntity.getBody();
            
            if (kosResponse == null) {
                throw KosConnectionException.apiError("KOS-STATUS-CHECK", 
                        String.valueOf(responseEntity.getStatusCodeValue()), "응답 데이터가 없습니다");
            }

            // KOS 응답을 내부 모델로 변환
            BillInquiryResponse response = convertKosResponseToBillResponse(kosResponse);

            log.info("KOS 상태 확인 완료 - 요청ID: {}, 상태: {}", requestId, response.getStatus());
            return response;

        } catch (Exception e) {
            log.error("KOS 상태 확인 오류 - 요청ID: {}, 오류: {}", requestId, e.getMessage(), e);
            throw new KosConnectionException("KOS-STATUS-CHECK", 
                    "KOS 상태 확인 중 오류가 발생했습니다", e);
        }
    }

    /**
     * KOS 상태 확인 Circuit Breaker Fallback 메소드
     */
    public BillInquiryResponse checkInquiryStatusFallback(String requestId, Exception ex) {
        log.warn("KOS 상태 확인 Circuit Breaker 작동 - 요청ID: {}, 오류: {}", requestId, ex.getMessage());

        // 상태 확인 실패시 처리 중 상태로 반환
        return BillInquiryResponse.builder()
                .requestId(requestId)
                .status(BillInquiryResponse.ProcessStatus.PROCESSING)
                .build();
    }

    /**
     * KOS Mock 응답을 내부 응답 모델로 변환
     */
    private BillInquiryResponse convertKosMockResponseToBillResponse(Map<String, Object> kosCommonResponse) {
        try {
            // KosCommonResponse에서 success와 data 추출
            Boolean success = (Boolean) kosCommonResponse.get("success");
            String resultCode = (String) kosCommonResponse.get("resultCode");
            Map<String, Object> data = (Map<String, Object>) kosCommonResponse.get("data");
            
            if (!Boolean.TRUE.equals(success) || !"0000".equals(resultCode) || data == null) {
                log.warn("KOS Mock 요금조회 실패 - success: {}, resultCode: {}", success, resultCode);
                return BillInquiryResponse.builder()
                        .requestId(data != null ? (String) data.get("requestId") : null)
                        .status(BillInquiryResponse.ProcessStatus.FAILED)
                        .build();
            }
            
            // data에서 실제 요금 정보 추출
            String procStatus = (String) data.get("procStatus");
            Map<String, Object> billInfo = (Map<String, Object>) data.get("billInfo");
            
            // 상태 변환
            BillInquiryResponse.ProcessStatus status = BillInquiryResponse.ProcessStatus.COMPLETED;
            if ("SUCCESS".equalsIgnoreCase(procStatus)) {
                status = BillInquiryResponse.ProcessStatus.COMPLETED;
            } else if ("PROCESSING".equalsIgnoreCase(procStatus)) {
                status = BillInquiryResponse.ProcessStatus.PROCESSING;
            } else if ("FAILED".equalsIgnoreCase(procStatus)) {
                status = BillInquiryResponse.ProcessStatus.FAILED;
            }
            
            BillInquiryResponse.BillInfo convertedBillInfo = null;
            if (billInfo != null && status == BillInquiryResponse.ProcessStatus.COMPLETED) {
                // 할인 정보 처리
                List<BillInquiryResponse.DiscountInfo> discounts = new ArrayList<>();
                Object discountAmount = billInfo.get("discountAmount");
                if (discountAmount != null && convertToInteger(discountAmount) > 0) {
                    discounts.add(BillInquiryResponse.DiscountInfo.builder()
                            .name("기본 할인")
                            .amount(convertToInteger(discountAmount))
                            .build());
                }
                
                convertedBillInfo = BillInquiryResponse.BillInfo.builder()
                        .productName((String) billInfo.get("productName"))
                        .contractInfo((String) billInfo.get("lineNumber"))
                        .billingMonth((String) billInfo.get("billingMonth"))
                        .totalAmount(convertToInteger(billInfo.get("totalFee")))
                        .discountInfo(discounts)
                        .usage(BillInquiryResponse.UsageInfo.builder()
                                .voice((String) billInfo.get("voiceUsage"))
                                .sms((String) billInfo.get("smsUsage"))
                                .data((String) billInfo.get("dataUsage"))
                                .build())
                        .terminationFee(0) // KOS Mock에서 기본값
                        .deviceInstallment(0) // KOS Mock에서 기본값
                        .paymentInfo(BillInquiryResponse.PaymentInfo.builder()
                                .billingDate((String) billInfo.get("dueDate"))
                                .paymentStatus(getBillPaymentStatus((String) billInfo.get("billStatus")))
                                .paymentMethod("자동이체")
                                .build())
                        .build();
            }
            
            return BillInquiryResponse.builder()
                    .requestId((String) data.get("requestId"))
                    .status(status)
                    .billInfo(convertedBillInfo)
                    .build();
                    
        } catch (Exception e) {
            log.error("KOS Mock 응답 변환 오류: {}", e.getMessage(), e);
            throw KosConnectionException.dataConversionError("KOS-BILL-INQUIRY", "BillInquiryResponse", e);
        }
    }
    
    /**
     * BigDecimal이나 다른 타입을 Integer로 변환
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
     * KOS Mock의 billStatus를 PaymentStatus로 변환
     */
    private BillInquiryResponse.PaymentStatus getBillPaymentStatus(String billStatus) {
        if (billStatus == null) return BillInquiryResponse.PaymentStatus.UNPAID;
        
        switch (billStatus.toUpperCase()) {
            case "PAID":
            case "CONFIRMED":
                return BillInquiryResponse.PaymentStatus.PAID;
            case "UNPAID":
                return BillInquiryResponse.PaymentStatus.UNPAID;
            case "OVERDUE":
                return BillInquiryResponse.PaymentStatus.OVERDUE;
            default:
                return BillInquiryResponse.PaymentStatus.UNPAID;
        }
    }

    /**
     * KOS 응답을 내부 응답 모델로 변환
     */
    private BillInquiryResponse convertKosResponseToBillResponse(KosResponse kosResponse) {
        try {
            // 상태 변환 - null 체크 추가
            BillInquiryResponse.ProcessStatus status;
            String kosStatus = kosResponse.getStatus();
            if (kosStatus == null || kosStatus.trim().isEmpty()) {
                log.warn("KOS 응답 상태가 null이거나 빈 문자열입니다. 기본값(PROCESSING)으로 설정합니다.");
                status = BillInquiryResponse.ProcessStatus.PROCESSING;
            } else {
                switch (kosStatus.toUpperCase()) {
                    case "SUCCESS":
                    case "COMPLETED":
                        status = BillInquiryResponse.ProcessStatus.COMPLETED;
                        break;
                    case "PROCESSING":
                    case "PENDING":
                        status = BillInquiryResponse.ProcessStatus.PROCESSING;
                        break;
                    case "FAILED":
                    case "ERROR":
                        status = BillInquiryResponse.ProcessStatus.FAILED;
                        break;
                    default:
                        log.warn("알 수 없는 KOS 상태: {}. 기본값(PROCESSING)으로 설정합니다.", kosStatus);
                        status = BillInquiryResponse.ProcessStatus.PROCESSING;
                        break;
                }
            }

            BillInquiryResponse.BillInfo billInfo = null;
            
            // 성공한 경우에만 요금 정보 변환
            if (status == BillInquiryResponse.ProcessStatus.COMPLETED && kosResponse.getBillData() != null) {
                // 할인 정보 변환
                List<BillInquiryResponse.DiscountInfo> discounts = new ArrayList<>();
                if (kosResponse.getBillData().getDiscounts() != null) {
                    kosResponse.getBillData().getDiscounts().forEach(discount -> 
                        discounts.add(BillInquiryResponse.DiscountInfo.builder()
                                .name(discount.getName())
                                .amount(discount.getAmount())
                                .build())
                    );
                }

                // 사용량 정보 변환
                BillInquiryResponse.UsageInfo usage = null;
                if (kosResponse.getBillData().getUsage() != null) {
                    usage = BillInquiryResponse.UsageInfo.builder()
                            .voice(kosResponse.getBillData().getUsage().getVoice())
                            .sms(kosResponse.getBillData().getUsage().getSms())
                            .data(kosResponse.getBillData().getUsage().getData())
                            .build();
                }

                // 납부 정보 변환
                BillInquiryResponse.PaymentInfo payment = null;
                if (kosResponse.getBillData().getPayment() != null) {
                    BillInquiryResponse.PaymentStatus paymentStatus;
                    switch (kosResponse.getBillData().getPayment().getStatus().toUpperCase()) {
                        case "PAID":
                            paymentStatus = BillInquiryResponse.PaymentStatus.PAID;
                            break;
                        case "UNPAID":
                            paymentStatus = BillInquiryResponse.PaymentStatus.UNPAID;
                            break;
                        case "OVERDUE":
                            paymentStatus = BillInquiryResponse.PaymentStatus.OVERDUE;
                            break;
                        default:
                            paymentStatus = BillInquiryResponse.PaymentStatus.UNPAID;
                            break;
                    }

                    payment = BillInquiryResponse.PaymentInfo.builder()
                            .billingDate(kosResponse.getBillData().getPayment().getBillingDate())
                            .paymentStatus(paymentStatus)
                            .paymentMethod(kosResponse.getBillData().getPayment().getMethod())
                            .build();
                }

                billInfo = BillInquiryResponse.BillInfo.builder()
                        .productName(kosResponse.getBillData().getProductName())
                        .contractInfo(kosResponse.getBillData().getContractInfo())
                        .billingMonth(kosResponse.getBillData().getBillingMonth())
                        .totalAmount(kosResponse.getBillData().getTotalAmount())
                        .discountInfo(discounts)
                        .usage(usage)
                        .terminationFee(kosResponse.getBillData().getTerminationFee())
                        .deviceInstallment(kosResponse.getBillData().getDeviceInstallment())
                        .paymentInfo(payment)
                        .build();
            }

            return BillInquiryResponse.builder()
                    .requestId(kosResponse.getRequestId())
                    .status(status)
                    .billInfo(billInfo)
                    .build();

        } catch (Exception e) {
            log.error("KOS 응답 변환 오류: {}", e.getMessage(), e);
            throw KosConnectionException.dataConversionError("KOS-BILL-INQUIRY", "BillInquiryResponse", e);
        }
    }

    /**
     * KOS Mock 직접 호출 Circuit Breaker Fallback 메소드
     */
    public KosBillInquiryResponse inquireBillDirectFallback(String lineNumber, String inquiryMonth, Exception ex) {
        log.warn("KOS Mock 직접 호출 Circuit Breaker 작동 - 회선: {}, 조회월: {}, 오류: {}", 
                lineNumber, inquiryMonth, ex.getMessage());

        // 기본 실패 응답 생성
        return KosBillInquiryResponse.builder()
                .requestId(generateRequestId())
                .procStatus("FAILED")
                .resultCode("9999")
                .resultMessage("시스템 오류로 인한 조회 실패")
                .build();
    }

    /**
     * Map을 KosBillInquiryResponse로 변환
     */
    private KosBillInquiryResponse convertMapToKosBillInquiryResponse(Map<String, Object> data) {
        try {
            // billInfo 데이터 처리
            KosBillInquiryResponse.BillInfo billInfo = null;
            Map<String, Object> billInfoMap = (Map<String, Object>) data.get("billInfo");
            if (billInfoMap != null) {
                billInfo = KosBillInquiryResponse.BillInfo.builder()
                        .lineNumber((String) billInfoMap.get("lineNumber"))
                        .billingMonth((String) billInfoMap.get("billingMonth"))
                        .productCode((String) billInfoMap.get("productCode"))
                        .productName((String) billInfoMap.get("productName"))
                        .monthlyFee(convertToBigDecimal(billInfoMap.get("monthlyFee")))
                        .usageFee(convertToBigDecimal(billInfoMap.get("usageFee")))
                        .discountAmount(convertToBigDecimal(billInfoMap.get("discountAmount")))
                        .totalFee(convertToBigDecimal(billInfoMap.get("totalFee")))
                        .dataUsage((String) billInfoMap.get("dataUsage"))
                        .voiceUsage((String) billInfoMap.get("voiceUsage"))
                        .smsUsage((String) billInfoMap.get("smsUsage"))
                        .billStatus((String) billInfoMap.get("billStatus"))
                        .dueDate((String) billInfoMap.get("dueDate"))
                        .build();
            }

            // customerInfo 데이터 처리
            KosBillInquiryResponse.CustomerInfo customerInfo = null;
            Map<String, Object> customerInfoMap = (Map<String, Object>) data.get("customerInfo");
            if (customerInfoMap != null) {
                customerInfo = KosBillInquiryResponse.CustomerInfo.builder()
                        .customerName((String) customerInfoMap.get("customerName"))
                        .customerId((String) customerInfoMap.get("customerId"))
                        .operatorCode((String) customerInfoMap.get("operatorCode"))
                        .lineStatus((String) customerInfoMap.get("lineStatus"))
                        .build();
            }

            return KosBillInquiryResponse.builder()
                    .requestId((String) data.get("requestId"))
                    .procStatus((String) data.get("procStatus"))
                    .resultCode((String) data.get("resultCode"))
                    .resultMessage((String) data.get("resultMessage"))
                    .billInfo(billInfo)
                    .customerInfo(customerInfo)
                    .build();

        } catch (Exception e) {
            log.error("Map을 KosBillInquiryResponse로 변환 실패: {}", e.getMessage(), e);
            throw new RuntimeException("응답 데이터 변환 실패", e);
        }
    }

    /**
     * Object를 BigDecimal로 변환
     */
    private java.math.BigDecimal convertToBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof java.math.BigDecimal) return (java.math.BigDecimal) value;
        if (value instanceof Number) return java.math.BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new java.math.BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            log.warn("BigDecimal 변환 실패: {}", value);
            return java.math.BigDecimal.ZERO;
        }
    }

    /**
     * 요청 ID 생성
     */
    private String generateRequestId() {
        String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("REQ_%s_%s", currentDate, uuid);
    }

    /**
     * 회선번호의 실제 요금 데이터가 있는 월 목록 조회
     * 
     * @param lineNumber 회선번호
     * @return 데이터가 있는 월 목록 (yyyy-MM 형식)
     */
    @CircuitBreaker(name = "kos-available-months", fallbackMethod = "getAvailableMonthsFallback")
    @Retry(name = "kos-available-months")
    public List<String> getAvailableMonths(String lineNumber) {
        log.info("KOS에서 회선 {}의 데이터 보유 월 조회", lineNumber);

        try {
            // 회선번호 형식 변환 (010-1234-5678 → 01012345678)
            String formattedLineNumber = lineNumber.replaceAll("-", "");
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Service-Name", "MVNO-BILL-INQUIRY");
            headers.set("X-Request-ID", java.util.UUID.randomUUID().toString());

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            // KOS Mock API 호출 - 월 목록 조회
            String kosUrl = kosProperties.getBaseUrl() + "/api/v1/kos/bill/available-months/" + formattedLineNumber;
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    kosUrl, HttpMethod.GET, requestEntity, Map.class
            );

            Map<String, Object> response = responseEntity.getBody();
            
            if (response == null) {
                log.warn("KOS에서 월 목록 응답이 없음");
                return new ArrayList<>();
            }

            // KosCommonResponse의 data 부분에서 월 목록 추출
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                log.warn("KOS 응답에서 data를 찾을 수 없음");
                return new ArrayList<>();
            }

            List<String> availableMonths = (List<String>) data.get("availableMonths");
            if (availableMonths == null) {
                availableMonths = new ArrayList<>();
            }

            log.info("KOS에서 조회된 데이터 보유 월: {} (총 {}개월)", availableMonths, availableMonths.size());
            return availableMonths;

        } catch (Exception e) {
            log.error("KOS에서 데이터 보유 월 조회 오류 - 회선: {}, 오류: {}", lineNumber, e.getMessage(), e);
            throw new KosConnectionException("KOS-AVAILABLE-MONTHS", 
                    "KOS 시스템에서 데이터 보유 월 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 데이터 보유 월 조회 Circuit Breaker Fallback 메소드
     */
    public List<String> getAvailableMonthsFallback(String lineNumber, Exception ex) {
        log.warn("KOS 데이터 보유 월 조회 Circuit Breaker 작동 - 회선: {}, 오류: {}", lineNumber, ex.getMessage());
        return new ArrayList<>(); // 빈 목록 반환
    }

    /**
     * KOS 시스템 연결 상태 확인
     * 
     * @return 연결 가능 여부
     */
    @CircuitBreaker(name = "kos-health-check")
    public boolean isKosSystemAvailable() {
        try {
            String healthUrl = kosProperties.getBaseUrl() + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            
            boolean available = response.getStatusCode().is2xxSuccessful();
            log.debug("KOS 시스템 상태 확인 - 사용가능: {}", available);
            
            return available;
        } catch (Exception e) {
            log.warn("KOS 시스템 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }
}