package com.phonebill.kosmock.data;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Mock 요금 데이터 모델
 * KOS 시스템의 요금 정보를 모방합니다.
 */
@Data
@Builder
public class MockBillData {
    
    /**
     * 회선번호
     */
    private String lineNumber;
    
    /**
     * 청구월 (YYYYMM)
     */
    private String billingMonth;
    
    /**
     * 상품 코드
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
     * 사용료
     */
    private BigDecimal usageFee;
    
    /**
     * 총 요금
     */
    private BigDecimal totalFee;
    
    /**
     * 데이터 사용량
     */
    private String dataUsage;
    
    /**
     * 음성 사용량
     */
    private String voiceUsage;
    
    /**
     * SMS 사용량
     */
    private String smsUsage;
    
    /**
     * 청구 상태 (PENDING, CONFIRMED, PAID)
     */
    private String billStatus;
    
    /**
     * 납부 기한 (YYYYMMDD)
     */
    private String dueDate;
    
    /**
     * 할인 금액
     */
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    /**
     * 부가세
     */
    @Builder.Default
    private BigDecimal vat = BigDecimal.ZERO;
    
    /**
     * 미납 금액
     */
    @Builder.Default
    private BigDecimal unpaidAmount = BigDecimal.ZERO;
}