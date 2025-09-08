package com.phonebill.bill.service;

import com.phonebill.bill.config.KosProperties;
import com.phonebill.bill.dto.BillInquiryResponse;
import com.phonebill.bill.exception.CircuitBreakerException;
import com.phonebill.bill.exception.KosConnectionException;
import com.phonebill.bill.external.KosRequest;
import com.phonebill.bill.external.KosResponse;
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
     * KOS 시스템에서 요금 정보 조회
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
                KosRequest kosRequest = KosRequest.builder()
                        .lineNumber(lineNumber)
                        .inquiryMonth(inquiryMonth)
                        .requestTime(LocalDateTime.now())
                        .build();

                // HTTP 헤더 설정
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                headers.set("X-Service-Name", "MVNO-BILL-INQUIRY");
                headers.set("X-Request-ID", java.util.UUID.randomUUID().toString());

                HttpEntity<KosRequest> requestEntity = new HttpEntity<>(kosRequest, headers);

                // KOS API 호출
                String kosUrl = kosProperties.getBaseUrl() + "/api/bill/inquiry";
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
     * KOS 응답을 내부 응답 모델로 변환
     */
    private BillInquiryResponse convertKosResponseToBillResponse(KosResponse kosResponse) {
        try {
            // 상태 변환
            BillInquiryResponse.ProcessStatus status;
            switch (kosResponse.getStatus().toUpperCase()) {
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
                    status = BillInquiryResponse.ProcessStatus.PROCESSING;
                    break;
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