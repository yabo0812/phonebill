package com.phonebill.kosmock.service;

import com.phonebill.kosmock.config.MockConfig;
import com.phonebill.kosmock.data.*;
import com.phonebill.kosmock.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

/**
 * KOS Mock 서비스
 * 실제 KOS 시스템의 동작을 모방합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KosMockService {

    private final MockDataService mockDataService;
    private final MockConfig mockConfig;
    private final Random random = new Random();

    /**
     * 요금 조회 처리 (Mock)
     */
    public KosBillInquiryResponse processBillInquiry(KosBillInquiryRequest request) {
        log.info("KOS Mock 요금 조회 요청 처리 시작 - RequestId: {}, LineNumber: {}", 
                request.getRequestId(), request.getLineNumber());
        
        // Mock 응답 지연 시뮬레이션
        simulateProcessingDelay();
        
        // Mock 실패 시뮬레이션
        if (shouldSimulateFailure()) {
            log.warn("KOS Mock 요금 조회 실패 시뮬레이션 - RequestId: {}", request.getRequestId());
            throw new RuntimeException("KOS 시스템 일시적 오류");
        }
        
        // 고객 데이터 조회
        MockCustomerData customerData = mockDataService.getCustomerData(request.getLineNumber());
        if (customerData == null) {
            log.warn("존재하지 않는 회선번호 - LineNumber: {}", request.getLineNumber());
            return createBillInquiryErrorResponse(request.getRequestId(), "1001", "존재하지 않는 회선번호입니다");
        }
        
        // 회선 상태 확인
        if (!"ACTIVE".equals(customerData.getLineStatus())) {
            log.warn("비활성 회선 - LineNumber: {}, Status: {}", 
                    request.getLineNumber(), customerData.getLineStatus());
            return createBillInquiryErrorResponse(request.getRequestId(), "1002", "비활성 상태의 회선입니다");
        }
        
        // 청구월 설정 (없으면 현재월 사용)
        String billingMonth = request.getBillingMonth();
        if (billingMonth == null || billingMonth.isEmpty()) {
            billingMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        }
        
        // 요금 데이터 조회
        MockBillData billData = mockDataService.getBillData(request.getLineNumber(), billingMonth);
        if (billData == null) {
            log.warn("해당 청구월 요금 정보 없음 - LineNumber: {}, BillingMonth: {}", 
                    request.getLineNumber(), billingMonth);
            return createBillInquiryErrorResponse(request.getRequestId(), "1003", "해당 월 요금 정보가 없습니다");
        }
        
        // 성공 응답 생성
        KosBillInquiryResponse response = KosBillInquiryResponse.builder()
                .requestId(request.getRequestId())
                .resultCode("0000")
                .resultMessage("정상 처리되었습니다")
                .billInfo(KosBillInquiryResponse.BillInfo.builder()
                        .lineNumber(billData.getLineNumber())
                        .billingMonth(billData.getBillingMonth())
                        .productCode(billData.getProductCode())
                        .productName(billData.getProductName())
                        .monthlyFee(billData.getMonthlyFee())
                        .usageFee(billData.getUsageFee())
                        .discountAmount(billData.getDiscountAmount())
                        .totalFee(billData.getTotalFee())
                        .dataUsage(billData.getDataUsage())
                        .voiceUsage(billData.getVoiceUsage())
                        .smsUsage(billData.getSmsUsage())
                        .billStatus(billData.getBillStatus())
                        .dueDate(billData.getDueDate())
                        .build())
                .customerInfo(KosBillInquiryResponse.CustomerInfo.builder()
                        .customerName(customerData.getCustomerName())
                        .customerId(customerData.getCustomerId())
                        .operatorCode(customerData.getOperatorCode())
                        .lineStatus(customerData.getLineStatus())
                        .build())
                .build();
        
        log.info("KOS Mock 요금 조회 처리 완료 - RequestId: {}", request.getRequestId());
        return response;
    }
    
    /**
     * 상품 변경 처리 (Mock)
     */
    public KosProductChangeResponse processProductChange(KosProductChangeRequest request) {
        log.info("KOS Mock 상품 변경 요청 처리 시작 - RequestId: {}, LineNumber: {}, Target: {}", 
                request.getRequestId(), request.getLineNumber(), request.getTargetProductCode());
        
        // Mock 응답 지연 시뮬레이션
        simulateProcessingDelay();
        
        // Mock 실패 시뮬레이션
        if (shouldSimulateFailure()) {
            log.warn("KOS Mock 상품 변경 실패 시뮬레이션 - RequestId: {}", request.getRequestId());
            throw new RuntimeException("KOS 시스템 일시적 오류");
        }
        
        // 고객 데이터 조회
        MockCustomerData customerData = mockDataService.getCustomerData(request.getLineNumber());
        if (customerData == null) {
            log.warn("존재하지 않는 회선번호 - LineNumber: {}", request.getLineNumber());
            return createProductChangeErrorResponse(request.getRequestId(), "2001", "존재하지 않는 회선번호입니다");
        }
        
        // 회선 상태 확인
        if (!"ACTIVE".equals(customerData.getLineStatus())) {
            log.warn("비활성 회선 - LineNumber: {}, Status: {}", 
                    request.getLineNumber(), customerData.getLineStatus());
            return createProductChangeErrorResponse(request.getRequestId(), "2002", "비활성 상태의 회선입니다");
        }
        
        // 현재 상품과 타겟 상품 조회
        MockProductData currentProduct = mockDataService.getProductData(request.getCurrentProductCode());
        MockProductData targetProduct = mockDataService.getProductData(request.getTargetProductCode());
        
        if (currentProduct == null || targetProduct == null) {
            log.warn("존재하지 않는 상품 코드 - Current: {}, Target: {}", 
                    request.getCurrentProductCode(), request.getTargetProductCode());
            return createProductChangeErrorResponse(request.getRequestId(), "2003", "존재하지 않는 상품 코드입니다");
        }
        
        // 타겟 상품 판매 상태 확인
        if (!"ACTIVE".equals(targetProduct.getStatus())) {
            log.warn("판매 중단된 상품 - ProductCode: {}, Status: {}", 
                    request.getTargetProductCode(), targetProduct.getStatus());
            return createProductChangeErrorResponse(request.getRequestId(), "2004", "판매가 중단된 상품입니다");
        }
        
        // 통신사업자 일치 확인
        if (!currentProduct.getOperatorCode().equals(targetProduct.getOperatorCode())) {
            log.warn("다른 통신사업자 상품으로 변경 시도 - Current: {}, Target: {}", 
                    currentProduct.getOperatorCode(), targetProduct.getOperatorCode());
            return createProductChangeErrorResponse(request.getRequestId(), "2005", "다른 통신사업자 상품으로는 변경할 수 없습니다");
        }
        
        // KOS 주문 번호 생성
        String kosOrderNumber = generateKosOrderNumber();
        
        // 적용 일자 설정 (없으면 내일 사용)
        String effectiveDate = request.getEffectiveDate();
        if (effectiveDate == null || effectiveDate.isEmpty()) {
            effectiveDate = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        
        // 성공 응답 생성
        KosProductChangeResponse response = KosProductChangeResponse.builder()
                .requestId(request.getRequestId())
                .resultCode("0000")
                .resultMessage("정상 처리되었습니다")
                .changeInfo(KosProductChangeResponse.ChangeInfo.builder()
                        .lineNumber(request.getLineNumber())
                        .previousProductCode(currentProduct.getProductCode())
                        .previousProductName(currentProduct.getProductName())
                        .newProductCode(targetProduct.getProductCode())
                        .newProductName(targetProduct.getProductName())
                        .effectiveDate(effectiveDate)
                        .changeStatus("SUCCESS")
                        .kosOrderNumber(kosOrderNumber)
                        .estimatedCompletionTime(LocalDateTime.now().plusMinutes(30)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))
                        .build())
                .build();
        
        // 처리 결과 저장
        MockProcessingResult processingResult = MockProcessingResult.builder()
                .requestId(request.getRequestId())
                .processingType("PRODUCT_CHANGE")
                .status("SUCCESS")
                .message("상품 변경이 성공적으로 처리되었습니다")
                .requestedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .processingTimeMs(mockConfig.getResponseDelay())
                .build();
        
        mockDataService.saveProcessingResult(request.getRequestId(), processingResult);
        
        log.info("KOS Mock 상품 변경 처리 완료 - RequestId: {}, KosOrderNumber: {}", 
                request.getRequestId(), kosOrderNumber);
        
        return response;
    }
    
    /**
     * 처리 지연 시뮬레이션
     */
    private void simulateProcessingDelay() {
        try {
            Thread.sleep(mockConfig.getResponseDelay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("처리 지연 시뮬레이션 중단", e);
        }
    }
    
    /**
     * 실패 시뮬레이션 여부 결정
     */
    private boolean shouldSimulateFailure() {
        return random.nextDouble() < mockConfig.getFailureRate();
    }
    
    /**
     * KOS 주문 번호 생성
     */
    private String generateKosOrderNumber() {
        return "KOS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) 
                + String.format("%03d", random.nextInt(1000));
    }
    
    /**
     * 요금 조회 오류 응답 생성
     */
    private KosBillInquiryResponse createBillInquiryErrorResponse(String requestId, String errorCode, String errorMessage) {
        return KosBillInquiryResponse.builder()
                .requestId(requestId)
                .resultCode(errorCode)
                .resultMessage(errorMessage)
                .build();
    }
    
    /**
     * 상품 변경 오류 응답 생성
     */
    private KosProductChangeResponse createProductChangeErrorResponse(String requestId, String errorCode, String errorMessage) {
        return KosProductChangeResponse.builder()
                .requestId(requestId)
                .resultCode(errorCode)
                .resultMessage(errorMessage)
                .build();
    }
}