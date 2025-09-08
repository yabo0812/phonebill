package com.unicorn.phonebill.product.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 상품 도메인 모델
 */
@Getter
@Builder
public class Product {
    
    private final String productCode;
    private final String productName;
    private final BigDecimal monthlyFee;
    private final String dataAllowance;
    private final String voiceAllowance;
    private final String smsAllowance;
    private final ProductStatus status;
    private final String operatorCode;
    private final String description;

    /**
     * 다른 상품으로 변경 가능한지 확인
     */
    public boolean canChangeTo(Product targetProduct) {
        if (targetProduct == null) {
            return false;
        }
        
        // 동일한 상품으로는 변경 불가
        if (this.productCode.equals(targetProduct.productCode)) {
            return false;
        }
        
        // 동일한 사업자 상품끼리만 변경 가능
        if (!isSameOperator(targetProduct)) {
            return false;
        }
        
        // 대상 상품이 판매 중이어야 함
        return targetProduct.status == ProductStatus.ACTIVE;
    }

    /**
     * 동일한 사업자 상품인지 확인
     */
    public boolean isSameOperator(Product other) {
        return other != null && 
               this.operatorCode != null && 
               this.operatorCode.equals(other.operatorCode);
    }

    /**
     * 상품이 활성 상태인지 확인
     */
    public boolean isActive() {
        return status == ProductStatus.ACTIVE;
    }

    /**
     * 상품이 판매 중지 상태인지 확인
     */
    public boolean isDiscontinued() {
        return status == ProductStatus.DISCONTINUED;
    }

    /**
     * 월 요금 차이 계산
     */
    public BigDecimal calculateFeeDifference(Product targetProduct) {
        if (targetProduct == null || targetProduct.monthlyFee == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal currentFee = this.monthlyFee != null ? this.monthlyFee : BigDecimal.ZERO;
        return targetProduct.monthlyFee.subtract(currentFee);
    }

    /**
     * 요금이 더 비싼지 확인
     */
    public boolean isMoreExpensiveThan(Product other) {
        if (other == null || other.monthlyFee == null || this.monthlyFee == null) {
            return false;
        }
        return this.monthlyFee.compareTo(other.monthlyFee) > 0;
    }

    /**
     * 프리미엄 상품인지 확인 (월 요금 기준)
     */
    public boolean isPremium() {
        if (monthlyFee == null) {
            return false;
        }
        // 월 요금 60,000원 이상을 프리미엄으로 간주
        return monthlyFee.compareTo(new BigDecimal("60000")) >= 0;
    }

    /**
     * 상품 정보 요약 문자열 생성
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(productName);
        if (monthlyFee != null) {
            sb.append(" (월 ").append(monthlyFee.toPlainString()).append("원)");
        }
        if (dataAllowance != null) {
            sb.append(" - 데이터: ").append(dataAllowance);
        }
        return sb.toString();
    }
}