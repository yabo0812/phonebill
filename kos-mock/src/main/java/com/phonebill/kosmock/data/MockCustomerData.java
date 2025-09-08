package com.phonebill.kosmock.data;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Mock 고객 데이터 모델
 * KOS 시스템의 고객 정보를 모방합니다.
 */
@Data
@Builder
public class MockCustomerData {
    
    /**
     * 회선번호 (Primary Key)
     */
    private String lineNumber;
    
    /**
     * 고객명
     */
    private String customerName;
    
    /**
     * 고객 ID
     */
    private String customerId;
    
    /**
     * 통신사업자 코드 (KT, SKT, LGU+ 등)
     */
    private String operatorCode;
    
    /**
     * 현재 상품 코드
     */
    private String currentProductCode;
    
    /**
     * 회선 상태 (ACTIVE, SUSPENDED, TERMINATED)
     */
    private String lineStatus;
    
    /**
     * 계약일시
     */
    private LocalDateTime contractDate;
    
    /**
     * 최종 수정일시
     */
    private LocalDateTime lastModified;
    
    /**
     * 고객 등급 (VIP, GOLD, SILVER, BRONZE)
     */
    @Builder.Default
    private String customerGrade = "SILVER";
    
    /**
     * 가입 유형 (INDIVIDUAL, CORPORATE)
     */
    @Builder.Default
    private String subscriptionType = "INDIVIDUAL";
}