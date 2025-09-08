package com.unicorn.phonebill.product.domain;

/**
 * 상품 상태
 */
public enum ProductStatus {
    /**
     * 판매 중
     */
    ACTIVE("판매 중"),
    
    /**
     * 판매 중지
     */
    DISCONTINUED("판매 중지"),
    
    /**
     * 준비 중
     */
    PREPARING("준비 중");

    private final String description;

    ProductStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 변경 가능한 상품 상태인지 확인
     */
    public boolean isChangeable() {
        return this == ACTIVE;
    }
}