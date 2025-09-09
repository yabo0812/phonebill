package com.phonebill.kosmock.service;

import com.phonebill.kosmock.config.MockConfig;
import com.phonebill.kosmock.data.*;
import com.phonebill.kosmock.data.MockDataService;
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
                .procStatus("SUCCESS")
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
        
        // 적용 일자 설정 (현재 날짜로 자동 설정)
        String effectiveDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 실제 고객 데이터 업데이트
        boolean updateSuccess = mockDataService.updateCustomerProduct(request.getLineNumber(), request.getTargetProductCode());
        if (!updateSuccess) {
            log.error("고객 상품 코드 업데이트 실패 - LineNumber: {}, TargetProduct: {}", 
                    request.getLineNumber(), request.getTargetProductCode());
            return createProductChangeErrorResponse(request.getRequestId(), "2099", "상품 변경 처리 중 오류가 발생했습니다");
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
     * 상품 목록 조회 (Mock)
     */
    public KosProductListResponse getProductList() {
        log.info("KOS Mock 상품 목록 조회 요청 처리 시작");
        
        // Mock 응답 지연 시뮬레이션
        simulateProcessingDelay();
        
        // Mock 실패 시뮬레이션
        if (shouldSimulateFailure()) {
            log.warn("KOS Mock 상품 목록 조회 실패 시뮬레이션");
            throw new RuntimeException("KOS 시스템 일시적 오류");
        }
        
        try {
            // Mock 데이터에서 상품 목록 조회
            java.util.List<MockProductData> productDataList = mockDataService.getAllProducts();
            
            // KosProductInfo 리스트로 변환
            java.util.List<KosProductInfo> productInfoList = productDataList.stream()
                    .map(this::convertToProductInfo)
                    .collect(java.util.stream.Collectors.toList());
            
            log.info("KOS Mock 상품 목록 조회 완료 - 상품 수: {}", productInfoList.size());
            
            return KosProductListResponse.builder()
                    .resultCode("0000")
                    .resultMessage("상품 목록 조회가 완료되었습니다")
                    .productCount(productInfoList.size())
                    .products(productInfoList)
                    .build();
                    
        } catch (Exception e) {
            log.error("KOS Mock 상품 목록 조회 처리 중 오류 발생", e);
            return KosProductListResponse.builder()
                    .resultCode("9999")
                    .resultMessage("시스템 오류가 발생했습니다")
                    .productCount(0)
                    .products(java.util.Collections.emptyList())
                    .build();
        }
    }
    
    /**
     * 가입상품 조회 처리 (Mock)
     */
    public KosProductInquiryResponse processProductInquiry(KosProductInquiryRequest request) {
        log.info("KOS Mock 가입상품 조회 요청 처리 시작 - RequestId: {}, LineNumber: {}", 
                request.getRequestId(), request.getLineNumber());
        
        // Mock 응답 지연 시뮬레이션
        simulateProcessingDelay();
        
        // Mock 실패 시뮬레이션
        if (shouldSimulateFailure()) {
            log.warn("KOS Mock 가입상품 조회 실패 시뮬레이션 - RequestId: {}", request.getRequestId());
            throw new RuntimeException("KOS 시스템 일시적 오류");
        }
        
        // 고객 데이터 조회
        MockCustomerData customerData = mockDataService.getCustomerData(request.getLineNumber());
        if (customerData == null) {
            log.warn("존재하지 않는 회선번호 - LineNumber: {}", request.getLineNumber());
            return createProductInquiryErrorResponse(request.getRequestId(), "3001", "존재하지 않는 회선번호입니다");
        }
        
        // 회선 상태 확인
        if (!"ACTIVE".equals(customerData.getLineStatus())) {
            log.warn("비활성 회선 - LineNumber: {}, Status: {}", 
                    request.getLineNumber(), customerData.getLineStatus());
            return createProductInquiryErrorResponse(request.getRequestId(), "3002", "비활성 상태의 회선입니다");
        }
        
        // 현재 상품 정보 조회
        MockProductData productData = mockDataService.getProductData(customerData.getCurrentProductCode());
        if (productData == null) {
            log.warn("존재하지 않는 상품 코드 - ProductCode: {}", customerData.getCurrentProductCode());
            return createProductInquiryErrorResponse(request.getRequestId(), "3003", "상품 정보를 찾을 수 없습니다");
        }
        
        // 성공 응답 생성
        KosProductInquiryResponse response = KosProductInquiryResponse.builder()
                .requestId(request.getRequestId())
                .procStatus("SUCCESS")
                .resultCode("0000")
                .resultMessage("정상 처리되었습니다")
                .productInfo(KosProductInquiryResponse.ProductInfo.builder()
                        .lineNumber(customerData.getLineNumber())
                        .currentProductCode(productData.getProductCode())
                        .currentProductName(productData.getProductName())
                        .monthlyFee(productData.getMonthlyFee())
                        .dataAllowance(productData.getDataAllowance())
                        .voiceAllowance(productData.getVoiceAllowance())
                        .smsAllowance(productData.getSmsAllowance())
                        .productStatus(productData.getStatus())
                        .contractDate(customerData.getContractDate())
                        .build())
                .customerInfo(KosProductInquiryResponse.CustomerInfo.builder()
                        .customerName(customerData.getCustomerName())
                        .customerId(customerData.getCustomerId())
                        .operatorCode(customerData.getOperatorCode())
                        .lineStatus(customerData.getLineStatus())
                        .build())
                .build();
        
        log.info("KOS Mock 가입상품 조회 처리 완료 - RequestId: {}", request.getRequestId());
        return response;
    }
    
    /**
     * 회선번호의 실제 요금 데이터가 있는 월 목록 조회 (Mock)
     */
    public KosAvailableMonthsResponse getAvailableMonths(String lineNumber) {
        log.info("KOS Mock 데이터 보유 월 목록 조회 - LineNumber: {}", lineNumber);
        
        // Mock 응답 지연 시뮬레이션
        simulateProcessingDelay();
        
        // 고객 데이터 조회
        MockCustomerData customerData = mockDataService.getCustomerData(lineNumber);
        if (customerData == null) {
            log.warn("존재하지 않는 회선번호 - LineNumber: {}", lineNumber);
            return KosAvailableMonthsResponse.builder()
                    .resultCode("1001")
                    .resultMessage("존재하지 않는 회선번호입니다")
                    .availableMonths(java.util.Collections.emptyList())
                    .build();
        }
        
        // 회선 상태 확인
        if (!"ACTIVE".equals(customerData.getLineStatus())) {
            log.warn("비활성 회선 - LineNumber: {}, Status: {}", 
                    lineNumber, customerData.getLineStatus());
            return KosAvailableMonthsResponse.builder()
                    .resultCode("1002")
                    .resultMessage("비활성 상태의 회선입니다")
                    .availableMonths(java.util.Collections.emptyList())
                    .build();
        }
        
        // Mock 데이터에서 실제 데이터가 있는 월 목록 조회
        java.util.List<String> availableMonths = mockDataService.getAvailableMonths(lineNumber);
        
        log.info("KOS Mock 데이터 보유 월 목록 조회 완료 - LineNumber: {}, 월 수: {}", 
                lineNumber, availableMonths.size());
        
        return KosAvailableMonthsResponse.builder()
                .resultCode("0000")
                .resultMessage("정상 처리되었습니다")
                .lineNumber(lineNumber)
                .availableMonths(availableMonths)
                .build();
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
                .procStatus("FAILED")
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
    
    /**
     * 가입상품 조회 오류 응답 생성
     */
    private KosProductInquiryResponse createProductInquiryErrorResponse(String requestId, String errorCode, String errorMessage) {
        return KosProductInquiryResponse.builder()
                .requestId(requestId)
                .procStatus("FAILED")
                .resultCode(errorCode)
                .resultMessage(errorMessage)
                .build();
    }
    
    /**
     * MockProductData를 KosProductInfo로 변환
     */
    private KosProductInfo convertToProductInfo(MockProductData productData) {
        return KosProductInfo.builder()
                .productCode(productData.getProductCode())
                .productName(productData.getProductName())
                .productType(productData.getPlanType())
                .monthlyFee(productData.getMonthlyFee().intValue())
                .dataAllowance(parseDataAllowance(productData.getDataAllowance()))
                .voiceAllowance(parseVoiceAllowance(productData.getVoiceAllowance()))
                .smsAllowance(parseSmsAllowance(productData.getSmsAllowance()))
                .networkType(productData.getNetworkType())
                .status(productData.getStatus())
                .description(productData.getDescription())
                .build();
    }
    
    /**
     * 데이터 허용량을 정수로 변환 (GB 단위)
     */
    private Integer parseDataAllowance(String dataAllowance) {
        if (dataAllowance == null || "무제한".equals(dataAllowance)) {
            return -1; // 무제한을 -1로 표현
        }
        try {
            return Integer.parseInt(dataAllowance.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 음성 허용량을 정수로 변환 (분 단위)
     */
    private Integer parseVoiceAllowance(String voiceAllowance) {
        if (voiceAllowance == null || "무제한".equals(voiceAllowance)) {
            return -1; // 무제한을 -1로 표현
        }
        try {
            return Integer.parseInt(voiceAllowance.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * SMS 허용량을 정수로 변환 (건 단위)
     */
    private Integer parseSmsAllowance(String smsAllowance) {
        if (smsAllowance == null || "무제한".equals(smsAllowance)) {
            return -1; // 무제한을 -1로 표현
        }
        try {
            return Integer.parseInt(smsAllowance.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}