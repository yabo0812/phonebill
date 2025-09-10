package com.phonebill.kosmock.service;

import com.phonebill.kosmock.dto.MockDataCreateRequest;
import com.phonebill.kosmock.dto.MockDataCreateResponse;
import com.phonebill.kosmock.entity.BillEntity;
import com.phonebill.kosmock.entity.CustomerEntity;
import com.phonebill.kosmock.entity.ProductEntity;
import com.phonebill.kosmock.repository.BillRepository;
import com.phonebill.kosmock.repository.CustomerRepository;
import com.phonebill.kosmock.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Mock 데이터 생성 서비스
 */
@Service
@RequiredArgsConstructor
public class MockDataCreateService {
    
    private static final Logger log = LoggerFactory.getLogger(MockDataCreateService.class);
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final BillRepository billRepository;
    private final com.phonebill.kosmock.data.MockDataService mockDataService;
    
    /**
     * Mock 데이터 생성 (고객 정보 + 요금 정보)
     */
    @Transactional
    public MockDataCreateResponse createMockData(MockDataCreateRequest request) {
        log.info("Mock 데이터 생성 시작 - CustomerId: {}, LineNumber: {}", 
            request.getCustomerId(), request.getLineNumber());
        
        // 1. 기존 데이터 존재 여부 확인
        if (customerRepository.existsByCustomerIdAndLineNumber(request.getCustomerId(), request.getLineNumber())) {
            throw new IllegalArgumentException("이미 존재하는 고객 정보입니다: " + request.getCustomerId() + ", " + request.getLineNumber());
        }
        
        // 2. 랜덤 상품 선택
        List<ProductEntity> activeProducts = productRepository.findByStatusOrderByMonthlyFeeDesc("ACTIVE");
        if (activeProducts.isEmpty()) {
            throw new IllegalStateException("활성화된 상품이 없습니다");
        }
        
        ProductEntity selectedProduct = selectRandomProduct(activeProducts);
        
        // 3. 고객 정보 생성
        CustomerEntity customer = createCustomer(request, selectedProduct);
        customerRepository.save(customer);
        
        // 4. 요금 정보 생성 (최근 6개월)
        List<BillEntity> bills = createBills(customer, selectedProduct);
        billRepository.saveAll(bills);
        
        log.info("Mock 데이터 생성 완료 - CustomerId: {}, LineNumber: {}, Product: {}, Bills: {}", 
            request.getCustomerId(), request.getLineNumber(), selectedProduct.getProductCode(), bills.size());
        
        return MockDataCreateResponse.builder()
            .customerId(customer.getCustomerId())
            .lineNumber(customer.getLineNumber())
            .currentProductCode(selectedProduct.getProductCode())
            .currentProductName(selectedProduct.getProductName())
            .billCountCreated(bills.size())
            .message("Mock 데이터가 성공적으로 생성되었습니다")
            .build();
    }
    
    /**
     * 랜덤 상품 선택 (가중치 적용)
     */
    private ProductEntity selectRandomProduct(List<ProductEntity> products) {
        Random random = new Random();
        
        // 가중치 적용: 프리미엄 상품(30%), 스탠다드 상품(70%)
        if (random.nextDouble() < 0.7) {
            // 스탠다드/베이직 상품 우선
            Optional<ProductEntity> basicProduct = products.stream()
                .filter(p -> p.getProductName().contains("베이직") || p.getProductName().contains("스탠다드"))
                .findFirst();
            if (basicProduct.isPresent()) {
                return basicProduct.get();
            }
        }
        
        // 랜덤 선택
        return products.get(random.nextInt(products.size()));
    }
    
    /**
     * 고객 정보 생성
     */
    private CustomerEntity createCustomer(MockDataCreateRequest request, ProductEntity product) {
        Random random = new Random();
        
        return CustomerEntity.builder()
            .lineNumber(request.getLineNumber())
            .customerId(request.getCustomerId())
            .operatorCode("KT")
            .currentProductCode(product.getProductCode())
            .lineStatus("ACTIVE")
            .contractDate(LocalDateTime.now().minusMonths(random.nextInt(24) + 1)) // 1~24개월 전 가입
            .build();
    }
    
    /**
     * 요금 정보 생성 (최근 6개월)
     */
    private List<BillEntity> createBills(CustomerEntity customer, ProductEntity product) {
        List<BillEntity> bills = new ArrayList<>();
        Random random = new Random();
        
        for (int month = 0; month < 6; month++) {
            LocalDateTime billDate = LocalDateTime.now().minusMonths(month);
            String billingMonth = billDate.format(DateTimeFormatter.ofPattern("yyyyMM"));
            
            // 사용료 계산 (랜덤)
            BigDecimal usageFee = new BigDecimal(random.nextInt(30000));
            BigDecimal totalFee = product.getMonthlyFee().add(usageFee);
            
            BillEntity bill = BillEntity.builder()
                .lineNumber(customer.getLineNumber())
                .billingMonth(billingMonth)
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .monthlyFee(product.getMonthlyFee())
                .usageFee(usageFee)
                .totalFee(totalFee)
                .dataUsage(generateDataUsage(product))
                .voiceUsage(generateVoiceUsage(product))
                .smsUsage(generateSmsUsage())
                .billStatus("CONFIRMED")
                .dueDate(billDate.plusDays(25).format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .build();
            
            bills.add(bill);
        }
        
        return bills;
    }
    
    private String generateDataUsage(ProductEntity product) {
        Random random = new Random();
        if ("무제한".equals(product.getDataAllowance())) {
            return random.nextInt(200) + "GB";
        } else {
            try {
                int allowance = Integer.parseInt(product.getDataAllowance().replace("GB", ""));
                return random.nextInt(allowance) + "GB";
            } catch (NumberFormatException e) {
                return "10GB"; // 기본값
            }
        }
    }
    
    private String generateVoiceUsage(ProductEntity product) {
        Random random = new Random();
        if ("무제한".equals(product.getVoiceAllowance())) {
            return random.nextInt(500) + "분";
        } else {
            try {
                int allowance = Integer.parseInt(product.getVoiceAllowance().replace("분", ""));
                return random.nextInt(allowance) + "분";
            } catch (NumberFormatException e) {
                return "100분"; // 기본값
            }
        }
    }
    
    private String generateSmsUsage() {
        Random random = new Random();
        return random.nextInt(100) + "건";
    }
    
    /**
     * 고객 가입상품정보 조회
     */
    public Object getCustomerProduct(String customerId, String lineNumber) {
        log.info("고객 상품정보 조회 - CustomerId: {}, LineNumber: {}", customerId, lineNumber);
        
        Optional<CustomerEntity> customer = customerRepository
            .findByCustomerIdAndLineNumber(customerId, lineNumber);
        
        if (customer.isEmpty()) {
            log.warn("고객 정보를 찾을 수 없음 - CustomerId: {}, LineNumber: {}", customerId, lineNumber);
            return null;
        }
        
        CustomerEntity customerEntity = customer.get();
        
        // 현재 상품 정보 조회
        Optional<ProductEntity> product = productRepository
            .findByProductCode(customerEntity.getCurrentProductCode());
        
        if (product.isEmpty()) {
            log.warn("상품 정보를 찾을 수 없음 - ProductCode: {}", customerEntity.getCurrentProductCode());
            return null;
        }
        
        ProductEntity productEntity = product.get();
        
        // 응답 데이터 구성
        return CustomerProductInfo.builder()
            .customerId(customerEntity.getCustomerId())
            .lineNumber(customerEntity.getLineNumber())
            .operatorCode(customerEntity.getOperatorCode())
            .lineStatus(customerEntity.getLineStatus())
            .contractDate(customerEntity.getContractDate())
            .currentProductCode(productEntity.getProductCode())
            .currentProductName(productEntity.getProductName())
            .monthlyFee(productEntity.getMonthlyFee())
            .dataAllowance(productEntity.getDataAllowance())
            .voiceAllowance(productEntity.getVoiceAllowance())
            .smsAllowance(productEntity.getSmsAllowance())
            .productStatus(productEntity.getStatus())
            .build();
    }
    
    /**
     * 고객 요금정보 조회
     */
    public Object getCustomerBill(String customerId, String lineNumber) {
        log.info("고객 요금정보 조회 - CustomerId: {}, LineNumber: {}", customerId, lineNumber);
        
        Optional<CustomerEntity> customer = customerRepository
            .findByCustomerIdAndLineNumber(customerId, lineNumber);
        
        if (customer.isEmpty()) {
            log.warn("고객 정보를 찾을 수 없음 - CustomerId: {}, LineNumber: {}", customerId, lineNumber);
            return null;
        }
        
        // 최근 6개월 요금 정보 조회
        List<BillEntity> bills = billRepository.findByLineNumberOrderByBillingMonthDesc(lineNumber);
        
        if (bills.isEmpty()) {
            log.warn("요금 정보를 찾을 수 없음 - LineNumber: {}", lineNumber);
            return null;
        }
        
        // 응답 데이터 구성
        List<CustomerBillInfo> billInfos = bills.stream()
            .map(bill -> CustomerBillInfo.builder()
                .billingMonth(bill.getBillingMonth())
                .productCode(bill.getProductCode())
                .productName(bill.getProductName())
                .monthlyFee(bill.getMonthlyFee())
                .usageFee(bill.getUsageFee())
                .totalFee(bill.getTotalFee())
                .dataUsage(bill.getDataUsage())
                .voiceUsage(bill.getVoiceUsage())
                .smsUsage(bill.getSmsUsage())
                .billStatus(bill.getBillStatus())
                .dueDate(bill.getDueDate())
                .build())
            .toList();
        
        return CustomerBillsInfo.builder()
            .customerId(customer.get().getCustomerId())
            .lineNumber(lineNumber)
            .billCount(billInfos.size())
            .bills(billInfos)
            .build();
    }
    
    /**
     * 고객 상품정보 응답 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class CustomerProductInfo {
        private String customerId;
        private String lineNumber;
        private String operatorCode;
        private String lineStatus;
        private LocalDateTime contractDate;
        private String currentProductCode;
        private String currentProductName;
        private BigDecimal monthlyFee;
        private String dataAllowance;
        private String voiceAllowance;
        private String smsAllowance;
        private String productStatus;
    }
    
    /**
     * 고객 요금정보 응답 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class CustomerBillsInfo {
        private String customerId;
        private String lineNumber;
        private int billCount;
        private List<CustomerBillInfo> bills;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class CustomerBillInfo {
        private String billingMonth;
        private String productCode;
        private String productName;
        private BigDecimal monthlyFee;
        private BigDecimal usageFee;
        private BigDecimal totalFee;
        private String dataUsage;
        private String voiceUsage;
        private String smsUsage;
        private String billStatus;
        private String dueDate;
    }
}