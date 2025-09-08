package com.phonebill.kosmock.data;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Mock 상품 데이터 모델
 * KOS 시스템의 상품 정보를 모방합니다.
 */
@Data
@Builder
public class MockProductData {
    
    /**
     * 상품 코드 (Primary Key)
     */
    private String productCode;
    
    /**
     * 상품명
     */
    private String productName;
    
    /**
     * 월 기본료
     */
    private BigDecimal monthlyFee;
    
    /**
     * 데이터 제공량 (예: "100GB", "무제한")
     */
    private String dataAllowance;
    
    /**
     * 음성 제공량 (예: "300분", "무제한")
     */
    private String voiceAllowance;
    
    /**
     * SMS 제공량 (예: "100건", "기본 제공")
     */
    private String smsAllowance;
    
    /**
     * 통신사업자 코드 (KT, SKT, LGU+ 등)
     */
    private String operatorCode;
    
    /**
     * 네트워크 타입 (5G, LTE, 3G)
     */
    private String networkType;
    
    /**
     * 상품 상태 (ACTIVE, DISCONTINUED)
     */
    private String status;
    
    /**
     * 상품 설명
     */
    private String description;
    
    /**
     * 최소 이용기간 (개월)
     */
    @Builder.Default
    private Integer minimumUsagePeriod = 12;
    
    /**
     * 약정 할인 가능 여부
     */
    @Builder.Default
    private Boolean discountAvailable = true;
    
    /**
     * 요금제 유형 (POSTPAID, PREPAID)
     */
    @Builder.Default
    private String planType = "POSTPAID";
}