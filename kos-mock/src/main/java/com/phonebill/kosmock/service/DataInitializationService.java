package com.phonebill.kosmock.service;

import com.phonebill.kosmock.entity.ProductEntity;
import com.phonebill.kosmock.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 애플리케이션 시작 시 초기 데이터 생성 서비스
 */
@Service
@RequiredArgsConstructor
public class DataInitializationService implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(DataInitializationService.class);
    private final ProductRepository productRepository;
    
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        initializeProductData();
    }
    
    /**
     * 상품 정보 초기화
     * 상품이 없을 때만 수행
     */
    private void initializeProductData() {
        long productCount = productRepository.countAllProducts();
        
        if (productCount == 0) {
            log.info("상품 정보가 없습니다. 초기 상품 데이터를 생성합니다...");
            
            List<ProductEntity> initialProducts = createInitialProducts();
            productRepository.saveAll(initialProducts);
            
            log.info("초기 상품 데이터 {}개 생성 완료", initialProducts.size());
        } else {
            log.info("기존 상품 정보 {}개가 존재합니다. 초기화를 건너뜁니다.", productCount);
        }
    }
    
    /**
     * 초기 상품 데이터 생성
     */
    private List<ProductEntity> createInitialProducts() {
        return Arrays.asList(
            // 5G 상품
            ProductEntity.builder()
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
                .build(),
                
            ProductEntity.builder()
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
                .build(),
                
            // LTE 상품
            ProductEntity.builder()
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
                .build(),
                
            ProductEntity.builder()
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
                .build(),
                
            // 종료된 상품 (변경 불가)
            ProductEntity.builder()
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
                .build()
        );
    }
}