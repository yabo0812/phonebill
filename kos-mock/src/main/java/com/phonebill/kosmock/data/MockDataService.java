package com.phonebill.kosmock.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KOS Mock 데이터 서비스
 * 통신요금 조회 및 상품변경에 필요한 Mock 데이터를 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MockDataService {

    // Mock 사용자 데이터 (회선번호 기반)
    private final Map<String, MockCustomerData> mockCustomers = new ConcurrentHashMap<>();
    
    // Mock 상품 데이터
    private final Map<String, MockProductData> mockProducts = new ConcurrentHashMap<>();
    
    // Mock 요금 데이터  
    private final Map<String, MockBillData> mockBills = new ConcurrentHashMap<>();
    
    // 요청 처리 이력
    private final Map<String, MockProcessingResult> processingResults = new ConcurrentHashMap<>();

    /**
     * 초기 Mock 데이터 생성
     */
    public void initializeMockData() {
        log.info("KOS Mock 데이터 초기화 시작");
        
        initializeMockProducts();
        initializeMockCustomers();
        initializeMockBills();
        
        log.info("KOS Mock 데이터 초기화 완료 - 고객: {}, 상품: {}, 요금: {}", 
            mockCustomers.size(), mockProducts.size(), mockBills.size());
    }
    
    /**
     * Mock 상품 데이터 초기화
     */
    private void initializeMockProducts() {
        // 5G 상품
        mockProducts.put("5G-PREMIUM-001", MockProductData.builder()
            .productCode("5G-PREMIUM-001")
            .productName("5G 프리미엄 플랜")
            .monthlyFee(new BigDecimal("89000"))
            .dataAllowance("무제한")
            .voiceAllowance("무제한")
            .smsAllowance("무제한")
            .operatorCode("KT")
            .networkType("5G")
            .status("ACTIVE")
            .description("5G 네트워크 무제한 프리미엄 요금제")
            .build());
            
        mockProducts.put("5G-STANDARD-001", MockProductData.builder()
            .productCode("5G-STANDARD-001")
            .productName("5G 스탠다드 플랜")
            .monthlyFee(new BigDecimal("69000"))
            .dataAllowance("100GB")
            .voiceAllowance("무제한")
            .smsAllowance("무제한")
            .operatorCode("KT")
            .networkType("5G")
            .status("ACTIVE")
            .description("5G 네트워크 스탠다드 요금제")
            .build());
            
        // LTE 상품
        mockProducts.put("LTE-PREMIUM-001", MockProductData.builder()
            .productCode("LTE-PREMIUM-001")
            .productName("LTE 프리미엄 플랜")
            .monthlyFee(new BigDecimal("59000"))
            .dataAllowance("50GB")
            .voiceAllowance("무제한")
            .smsAllowance("무제한")
            .operatorCode("KT")
            .networkType("LTE")
            .status("ACTIVE")
            .description("LTE 네트워크 프리미엄 요금제")
            .build());
            
        mockProducts.put("LTE-BASIC-001", MockProductData.builder()
            .productCode("LTE-BASIC-001")
            .productName("LTE 베이직 플랜")
            .monthlyFee(new BigDecimal("39000"))
            .dataAllowance("20GB")
            .voiceAllowance("무제한")
            .smsAllowance("기본 제공")
            .operatorCode("KT")
            .networkType("LTE")
            .status("ACTIVE")
            .description("LTE 네트워크 베이직 요금제")
            .build());
            
        // 종료된 상품 (변경 불가)
        mockProducts.put("3G-OLD-001", MockProductData.builder()
            .productCode("3G-OLD-001")
            .productName("3G 레거시 플랜")
            .monthlyFee(new BigDecimal("29000"))
            .dataAllowance("5GB")
            .voiceAllowance("500분")
            .smsAllowance("100건")
            .operatorCode("KT")
            .networkType("3G")
            .status("DISCONTINUED")
            .description("3G 네트워크 레거시 요금제 (신규 가입 불가)")
            .build());
    }
    
    /**
     * Mock 고객 데이터 초기화
     */
    private void initializeMockCustomers() {
        // 테스트용 고객 데이터
        String[] testNumbers = {
            "01012345678", "01087654321", "01055554444", 
            "01099998888", "01077776666", "01033332222"
        };
        
        String[] testNames = {
            "김테스트", "이샘플", "박데모", "최모의", "정시험", "한실험"
        };
        
        String[] currentProducts = {
            "5G-PREMIUM-001", "5G-STANDARD-001", "LTE-PREMIUM-001", 
            "LTE-BASIC-001", "3G-OLD-001", "5G-PREMIUM-001"
        };
        
        for (int i = 0; i < testNumbers.length; i++) {
            mockCustomers.put(testNumbers[i], MockCustomerData.builder()
                .lineNumber(testNumbers[i])
                .customerName(testNames[i])
                .customerId("CUST" + String.format("%06d", i + 1))
                .operatorCode("KT")
                .currentProductCode(currentProducts[i])
                .lineStatus("ACTIVE")
                .contractDate(LocalDateTime.now().minusMonths(12 + i))
                .lastModified(LocalDateTime.now().minusDays(i))
                .build());
        }
        
        // 비활성 회선 테스트용
        mockCustomers.put("01000000000", MockCustomerData.builder()
            .lineNumber("01000000000")
            .customerName("비활성사용자")
            .customerId("CUST999999")
            .operatorCode("KT")
            .currentProductCode("LTE-BASIC-001")
            .lineStatus("SUSPENDED")
            .contractDate(LocalDateTime.now().minusMonths(6))
            .lastModified(LocalDateTime.now().minusDays(30))
            .build());
    }
    
    /**
     * Mock 요금 데이터 초기화
     */
    private void initializeMockBills() {
        for (MockCustomerData customer : mockCustomers.values()) {
            MockProductData product = mockProducts.get(customer.getCurrentProductCode());
            if (product != null) {
                // 최근 3개월 요금 데이터 생성
                for (int month = 0; month < 3; month++) {
                    LocalDateTime billDate = LocalDateTime.now().minusMonths(month);
                    String billKey = customer.getLineNumber() + "_" + billDate.format(DateTimeFormatter.ofPattern("yyyyMM"));
                    
                    BigDecimal usageFee = calculateUsageFee(product, month);
                    BigDecimal totalFee = product.getMonthlyFee().add(usageFee);
                    
                    mockBills.put(billKey, MockBillData.builder()
                        .lineNumber(customer.getLineNumber())
                        .billingMonth(billDate.format(DateTimeFormatter.ofPattern("yyyyMM")))
                        .productCode(product.getProductCode())
                        .productName(product.getProductName())
                        .monthlyFee(product.getMonthlyFee())
                        .usageFee(usageFee)
                        .totalFee(totalFee)
                        .dataUsage(generateRandomDataUsage(product))
                        .voiceUsage(generateRandomVoiceUsage(product))
                        .smsUsage(generateRandomSmsUsage())
                        .billStatus("CONFIRMED")
                        .dueDate(billDate.plusDays(25).format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                        .build());
                }
            }
        }
    }
    
    private BigDecimal calculateUsageFee(MockProductData product, int month) {
        // 간단한 사용료 계산 로직 (랜덤하게 0~30000원)
        Random random = new Random();
        return new BigDecimal(random.nextInt(30000));
    }
    
    private String generateRandomDataUsage(MockProductData product) {
        Random random = new Random();
        if ("무제한".equals(product.getDataAllowance())) {
            return random.nextInt(200) + "GB";
        } else {
            int allowance = Integer.parseInt(product.getDataAllowance().replace("GB", ""));
            return random.nextInt(allowance) + "GB";
        }
    }
    
    private String generateRandomVoiceUsage(MockProductData product) {
        Random random = new Random();
        if ("무제한".equals(product.getVoiceAllowance())) {
            return random.nextInt(500) + "분";
        } else {
            int allowance = Integer.parseInt(product.getVoiceAllowance().replace("분", ""));
            return random.nextInt(allowance) + "분";
        }
    }
    
    private String generateRandomSmsUsage() {
        Random random = new Random();
        return random.nextInt(100) + "건";
    }
    
    // Getter methods
    public MockCustomerData getCustomerData(String lineNumber) {
        return mockCustomers.get(lineNumber);
    }
    
    public MockProductData getProductData(String productCode) {
        return mockProducts.get(productCode);
    }
    
    public MockBillData getBillData(String lineNumber, String billingMonth) {
        return mockBills.get(lineNumber + "_" + billingMonth);
    }
    
    public List<MockProductData> getAllAvailableProducts() {
        return mockProducts.values().stream()
            .filter(product -> "ACTIVE".equals(product.getStatus()))
            .sorted(Comparator.comparing(MockProductData::getMonthlyFee).reversed())
            .toList();
    }
    
    public void saveProcessingResult(String requestId, MockProcessingResult result) {
        processingResults.put(requestId, result);
    }
    
    public MockProcessingResult getProcessingResult(String requestId) {
        return processingResults.get(requestId);
    }
    
    public List<MockBillData> getBillHistory(String lineNumber) {
        return mockBills.values().stream()
            .filter(bill -> lineNumber.equals(bill.getLineNumber()))
            .sorted(Comparator.comparing(MockBillData::getBillingMonth).reversed())
            .toList();
    }
}