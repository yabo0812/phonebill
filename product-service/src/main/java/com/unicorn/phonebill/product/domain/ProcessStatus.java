package com.unicorn.phonebill.product.domain;

/**
 * 상품변경 처리 상태
 */
public enum ProcessStatus {
    /**
     * 요청 접수
     */
    REQUESTED("요청 접수"),
    
    /**
     * 사전체크 완료
     */
    VALIDATED("사전체크 완료"),
    
    /**
     * 처리 중
     */
    PROCESSING("처리 중"),
    
    /**
     * 완료
     */
    COMPLETED("완료"),
    
    /**
     * 실패
     */
    FAILED("실패");

    private final String description;

    ProcessStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 처리가 완료된 상태인지 확인
     */
    public boolean isFinished() {
        return this == COMPLETED || this == FAILED;
    }

    /**
     * 성공적으로 완료된 상태인지 확인
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    /**
     * 처리 중인 상태인지 확인
     */
    public boolean isInProgress() {
        return this == PROCESSING || this == VALIDATED;
    }
}