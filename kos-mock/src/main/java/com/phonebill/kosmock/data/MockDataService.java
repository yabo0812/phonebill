package com.phonebill.kosmock.data;

import com.phonebill.kosmock.entity.BillEntity;
import com.phonebill.kosmock.entity.CustomerEntity;
import com.phonebill.kosmock.entity.ProductEntity;
import com.phonebill.kosmock.repository.BillRepository;
import com.phonebill.kosmock.repository.CustomerRepository;
import com.phonebill.kosmock.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Arrays;

/**
 * KOS Mock 데이터 서비스 (H2 데이터베이스 기반)
 * 통신요금 조회 및 상품변경에 필요한 Mock 데이터를 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MockDataService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final BillRepository billRepository;

    // 요청 처리 이력 (메모리 기반 유지)
    private final Map<String, MockProcessingResult> processingResults = new HashMap<>();

    /**
     * 초기 Mock 데이터 생성 (user-service 기반)
     */
    @Transactional
    public void initializeMockData() {
        log.info("KOS Mock 데이터 초기화 시작");
        
        // 상품 데이터만 초기화 (고객 데이터는 API 요청 시 동적 생성)
        initializeMockProducts();
        
        long productCount = productRepository.count();
        
        log.info("KOS Mock 데이터 초기화 완료 - 상품: {}", productCount);
    }
    
    
    // 기존 메소드들 - H2 데이터베이스 기반으로 재구현
    public MockCustomerData getCustomerData(String lineNumber) {
        log.info("MockDataService: 고객 데이터 조회 - LineNumber: {}", lineNumber);
        Optional<CustomerEntity> customerOpt = customerRepository.findByLineNumber(lineNumber);
        
        if (customerOpt.isEmpty()) {
            log.warn("MockDataService: 고객 정보를 찾을 수 없음 - LineNumber: {}", lineNumber);
            return null;
        }
        
        CustomerEntity entity = customerOpt.get();
        log.info("MockDataService: 고객 정보 발견 - CustomerId: {}, LineNumber: {}", 
                entity.getCustomerId(), entity.getLineNumber());
        
        return convertToMockCustomerData(entity);
    }
    
    public MockProductData getProductData(String productCode) {
        Optional<ProductEntity> productOpt = productRepository.findById(productCode);
        return productOpt.map(this::convertToMockProductData).orElse(null);
    }
    
    public MockBillData getBillData(String lineNumber, String billingMonth) {
        Optional<BillEntity> billOpt = billRepository.findByLineNumberAndBillingMonth(lineNumber, billingMonth);
        return billOpt.map(this::convertToMockBillData).orElse(null);
    }
    
    public List<MockProductData> getAllAvailableProducts() {
        List<ProductEntity> products = productRepository.findByStatusOrderByMonthlyFeeDesc("ACTIVE");
        return products.stream()
            .map(this::convertToMockProductData)
            .toList();
    }
    
    public List<MockProductData> getAllProducts() {
        List<ProductEntity> products = productRepository.findAll();
        return products.stream()
            .map(this::convertToMockProductData)
            .toList();
    }
    
    public void saveProcessingResult(String requestId, MockProcessingResult result) {
        processingResults.put(requestId, result);
    }
    
    public MockProcessingResult getProcessingResult(String requestId) {
        return processingResults.get(requestId);
    }
    
    public List<MockBillData> getBillHistory(String lineNumber) {
        List<BillEntity> bills = billRepository.findByLineNumberOrderByBillingMonthDesc(lineNumber);
        return bills.stream()
            .map(this::convertToMockBillData)
            .toList();
    }
    
    /**
     * 회선번호의 실제 요금 데이터가 있는 월 목록 조회
     */
    public List<String> getAvailableMonths(String lineNumber) {
        log.info("회선 {}의 실제 데이터 보유 월 조회", lineNumber);
        
        // 데이터베이스에서 실제 청구 데이터가 있는 월 목록 조회
        List<BillEntity> bills = billRepository.findByLineNumberOrderByBillingMonthDesc(lineNumber);
        
        if (bills.isEmpty()) {
            // 실제 데이터가 없으면 Mock 데이터를 생성하여 최근 3개월 반환
            log.info("실제 청구 데이터가 없어 Mock 데이터 생성: {}", lineNumber);
            createMockBillDataForRecentMonths(lineNumber);
            // 다시 조회
            bills = billRepository.findByLineNumberOrderByBillingMonthDesc(lineNumber);
        }
        
        // 청구월을 yyyy-MM 형식으로 변환하여 반환
        List<String> availableMonths = bills.stream()
            .map(bill -> {
                String billingMonth = bill.getBillingMonth();
                // yyyyMM 형식을 yyyy-MM 형식으로 변환
                if (billingMonth.length() == 6) {
                    return billingMonth.substring(0, 4) + "-" + billingMonth.substring(4, 6);
                }
                return billingMonth;
            })
            .distinct()
            .sorted(java.util.Collections.reverseOrder()) // 최신 월부터
            .toList();
        
        log.info("회선 {}의 데이터 보유 월: {} (총 {}개월)", lineNumber, availableMonths, availableMonths.size());
        return availableMonths;
    }
    
    /**
     * 최근 3개월 Mock 청구 데이터 생성
     */
    @Transactional
    private void createMockBillDataForRecentMonths(String lineNumber) {
        log.info("회선 {}의 Mock 청구 데이터 생성", lineNumber);
        
        // 고객 정보 조회
        Optional<CustomerEntity> customerOpt = customerRepository.findByLineNumberWithProduct(lineNumber);
        if (customerOpt.isEmpty()) {
            log.warn("회선번호 {}에 대한 고객 정보가 없어 청구 데이터를 생성할 수 없습니다", lineNumber);
            return;
        }
        
        CustomerEntity customer = customerOpt.get();
        
        // 현재 상품 정보 조회
        Optional<ProductEntity> productOpt = productRepository.findById(customer.getCurrentProductCode());
        if (productOpt.isEmpty()) {
            log.warn("상품 코드 {}를 찾을 수 없어 청구 데이터를 생성할 수 없습니다", customer.getCurrentProductCode());
            return;
        }
        
        ProductEntity product = productOpt.get();
        
        // 최근 3개월 청구 데이터 생성
        LocalDateTime now = LocalDateTime.now();
        List<BillEntity> mockBills = new ArrayList<>();
        
        for (int i = 0; i < 3; i++) {
            LocalDateTime monthDate = now.minusMonths(i);
            String billingMonth = monthDate.format(DateTimeFormatter.ofPattern("yyyyMM"));
            
            // 이미 해당 월 데이터가 있는지 확인
            if (billRepository.findByLineNumberAndBillingMonth(lineNumber, billingMonth).isPresent()) {
                continue;
            }
            
            // Mock 청구 데이터 생성
            BigDecimal usageFee = generateRandomUsageFee();
            BillEntity billEntity = BillEntity.builder()
                .lineNumber(lineNumber)
                .billingMonth(billingMonth)
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .monthlyFee(product.getMonthlyFee())
                .usageFee(usageFee)
                .totalFee(product.getMonthlyFee().add(usageFee))
                .dataUsage(generateRandomDataUsage())
                .voiceUsage(generateRandomVoiceUsage())
                .smsUsage(generateRandomSmsUsage())
                .billStatus(i == 0 ? "UNPAID" : "PAID") // 당월만 미납
                .dueDate(monthDate.plusDays(25).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .build();
            
            mockBills.add(billEntity);
        }
        
        if (!mockBills.isEmpty()) {
            billRepository.saveAll(mockBills);
            log.info("회선 {}의 Mock 청구 데이터 {}개 생성 완료", lineNumber, mockBills.size());
        }
    }
    
    // Mock 데이터 생성을 위한 헬퍼 메소드들
    private BigDecimal generateRandomUsageFee() {
        Random random = new Random();
        return new BigDecimal(random.nextInt(20000)); // 0~20,000원
    }
    
    private String generateRandomDataUsage() {
        Random random = new Random();
        double usage = random.nextDouble() * 100; // 0~100GB
        return String.format("%.1fGB", usage);
    }
    
    private String generateRandomVoiceUsage() {
        Random random = new Random();
        int minutes = random.nextInt(500); // 0~500분
        return String.format("%d분", minutes);
    }
    
    private String generateRandomSmsUsage() {
        Random random = new Random();
        int count = random.nextInt(100); // 0~100건
        return String.format("%d건", count);
    }
    
    // 엔티티를 Mock 데이터로 변환하는 메소드들
    private MockCustomerData convertToMockCustomerData(CustomerEntity entity) {
        return MockCustomerData.builder()
            .lineNumber(entity.getLineNumber())
            .customerName("Mock_Customer") // 고객명은 저장하지 않음
            .customerId(entity.getCustomerId())
            .operatorCode(entity.getOperatorCode())
            .currentProductCode(entity.getCurrentProductCode())
            .lineStatus(entity.getLineStatus())
            .contractDate(entity.getContractDate())
            .lastModified(entity.getUpdatedAt())
            .build();
    }
    
    private MockProductData convertToMockProductData(ProductEntity entity) {
        return MockProductData.builder()
            .productCode(entity.getProductCode())
            .productName(entity.getProductName())
            .monthlyFee(entity.getMonthlyFee())
            .dataAllowance(entity.getDataAllowance())
            .voiceAllowance(entity.getVoiceAllowance())
            .smsAllowance(entity.getSmsAllowance())
            .operatorCode(entity.getOperatorCode())
            .networkType(entity.getNetworkType())
            .status(entity.getStatus())
            .description(entity.getDescription())
            .build();
    }
    
    private MockBillData convertToMockBillData(BillEntity entity) {
        return MockBillData.builder()
            .lineNumber(entity.getLineNumber())
            .billingMonth(entity.getBillingMonth())
            .productCode(entity.getProductCode())
            .productName(entity.getProductName())
            .monthlyFee(entity.getMonthlyFee())
            .usageFee(entity.getUsageFee())
            .totalFee(entity.getTotalFee())
            .dataUsage(entity.getDataUsage())
            .voiceUsage(entity.getVoiceUsage())
            .smsUsage(entity.getSmsUsage())
            .billStatus(entity.getBillStatus())
            .dueDate(entity.getDueDate())
            .discountAmount(BigDecimal.ZERO) // BillEntity에 없으므로 기본값
            .build();
    }
    
    /**
     * Mock 상품 데이터 초기화
     */
    @Transactional
    private void initializeMockProducts() {
        log.info("Mock 상품 데이터 초기화 시작");
        
        // 기존 상품이 있으면 초기화하지 않음
        if (productRepository.count() > 0) {
            log.info("이미 상품 데이터가 존재합니다. 초기화를 건너뜁니다.");
            return;
        }
        
        // 기본 상품 데이터 생성
        List<ProductEntity> products = Arrays.asList(
            ProductEntity.builder()
                .productCode("5G-PREMIUM-001")
                .productName("5G 프리미엄")
                .monthlyFee(new BigDecimal("89000"))
                .dataAllowance("무제한")
                .voiceAllowance("무제한")
                .smsAllowance("무제한")
                .operatorCode("KT")
                .networkType("5G")
                .status("ACTIVE")
                .description("5G 프리미엄 요금제")
                .build(),
                
            ProductEntity.builder()
                .productCode("5G-STANDARD-001")
                .productName("5G 스탠다드")
                .monthlyFee(new BigDecimal("65000"))
                .dataAllowance("100GB")
                .voiceAllowance("무제한")
                .smsAllowance("무제한")
                .operatorCode("KT")
                .networkType("5G")
                .status("ACTIVE")
                .description("5G 스탠다드 요금제")
                .build(),
                
            ProductEntity.builder()
                .productCode("LTE-PREMIUM-001")
                .productName("LTE 프리미엄")
                .monthlyFee(new BigDecimal("55000"))
                .dataAllowance("무제한")
                .voiceAllowance("무제한")
                .smsAllowance("무제한")
                .operatorCode("KT")
                .networkType("LTE")
                .status("ACTIVE")
                .description("LTE 프리미엄 요금제")
                .build(),
                
            ProductEntity.builder()
                .productCode("LTE-BASIC-001")
                .productName("LTE 베이직")
                .monthlyFee(new BigDecimal("35000"))
                .dataAllowance("50GB")
                .voiceAllowance("300분")
                .smsAllowance("100건")
                .operatorCode("KT")
                .networkType("LTE")
                .status("ACTIVE")
                .description("LTE 베이직 요금제")
                .build(),
                
            ProductEntity.builder()
                .productCode("3G-OLD-001")
                .productName("3G 기본")
                .monthlyFee(new BigDecimal("25000"))
                .dataAllowance("10GB")
                .voiceAllowance("200분")
                .smsAllowance("50건")
                .operatorCode("KT")
                .networkType("3G")
                .status("INACTIVE")
                .description("3G 기본 요금제 (단종)")
                .build()
        );
        
        productRepository.saveAll(products);
        log.info("Mock 상품 데이터 {}개 생성 완료", products.size());
    }
    
    /**
     * 고객의 상품 코드 업데이트
     */
    @Transactional
    public boolean updateCustomerProduct(String lineNumber, String newProductCode) {
        log.info("고객 상품 코드 업데이트 - LineNumber: {}, NewProductCode: {}", lineNumber, newProductCode);
        
        // 고객 정보 조회
        Optional<CustomerEntity> customerOpt = customerRepository.findByLineNumber(lineNumber);
        if (customerOpt.isEmpty()) {
            log.warn("존재하지 않는 회선번호 - LineNumber: {}", lineNumber);
            return false;
        }
        
        CustomerEntity customer = customerOpt.get();
        String oldProductCode = customer.getCurrentProductCode();
        
        // 상품 코드 업데이트
        customer.setCurrentProductCode(newProductCode);
        customer.setUpdatedAt(LocalDateTime.now());
        
        // 저장
        customerRepository.save(customer);
        
        log.info("고객 상품 코드 업데이트 완료 - LineNumber: {}, {} -> {}", 
                lineNumber, oldProductCode, newProductCode);
        
        return true;
    }
}