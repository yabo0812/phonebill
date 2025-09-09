package com.unicorn.phonebill.product.domain;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 상품변경 처리 결과 도메인 모델
 * Updated for compilation fix
 */
public class ProductChangeResult {
    
    private final String requestId;
    private final boolean success;
    private final String resultCode;
    private final String resultMessage;
    private final String failureReason;
    private final String kosOrderNumber;
    private final String effectiveDate;
    private final Product changedProduct;
    private final LocalDateTime processedAt;
    private final Map<String, Object> additionalData;
    private final Map<String, Object> kosResponseData;

    // Builder 패턴용 생성자
    private ProductChangeResult(Builder builder) {
        this.requestId = builder.requestId;
        this.success = builder.success;
        this.resultCode = builder.resultCode;
        this.resultMessage = builder.resultMessage;
        this.failureReason = builder.failureReason;
        this.kosOrderNumber = builder.kosOrderNumber;
        this.effectiveDate = builder.effectiveDate;
        this.changedProduct = builder.changedProduct;
        this.processedAt = builder.processedAt;
        this.additionalData = builder.additionalData;
        this.kosResponseData = builder.kosResponseData;
    }

    // Getter 메서드들
    public String getRequestId() {
        return requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getKosOrderNumber() {
        return kosOrderNumber;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public Product getChangedProduct() {
        return changedProduct;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public Map<String, Object> getKosResponseData() {
        return kosResponseData;
    }

    public boolean isFailure() {
        return !success;
    }

    // Builder 패턴
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String requestId;
        private boolean success;
        private String resultCode;
        private String resultMessage;
        private String failureReason;
        private String kosOrderNumber;
        private String effectiveDate;
        private Product changedProduct;
        private LocalDateTime processedAt;
        private Map<String, Object> additionalData;
        private Map<String, Object> kosResponseData;

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder resultCode(String resultCode) {
            this.resultCode = resultCode;
            return this;
        }

        public Builder resultMessage(String resultMessage) {
            this.resultMessage = resultMessage;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder kosOrderNumber(String kosOrderNumber) {
            this.kosOrderNumber = kosOrderNumber;
            return this;
        }

        public Builder effectiveDate(String effectiveDate) {
            this.effectiveDate = effectiveDate;
            return this;
        }

        public Builder changedProduct(Product changedProduct) {
            this.changedProduct = changedProduct;
            return this;
        }

        public Builder processedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public Builder additionalData(Map<String, Object> additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        public Builder kosResponseData(Map<String, Object> kosResponseData) {
            this.kosResponseData = kosResponseData;
            return this;
        }

        public ProductChangeResult build() {
            return new ProductChangeResult(this);
        }
    }

    /**
     * 성공 결과 생성 (팩토리 메소드)
     */
    public static ProductChangeResult createSuccessResult(
            String requestId, 
            String resultMessage, 
            Product changedProduct) {
        
        return ProductChangeResult.builder()
                .requestId(requestId)
                .success(true)
                .resultCode("SUCCESS")
                .resultMessage(resultMessage)
                .changedProduct(changedProduct)
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 실패 결과 생성 (팩토리 메소드)
     */
    public static ProductChangeResult createFailureResult(
            String requestId, 
            String resultCode, 
            String resultMessage) {
        
        return ProductChangeResult.builder()
                .requestId(requestId)
                .success(false)
                .resultCode(resultCode)
                .resultMessage(resultMessage)
                .failureReason(resultMessage)
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 추가 데이터와 함께 실패 결과 생성
     */
    public static ProductChangeResult createFailureResult(
            String requestId, 
            String resultCode, 
            String resultMessage,
            Map<String, Object> additionalData) {
        
        return ProductChangeResult.builder()
                .requestId(requestId)
                .success(false)
                .resultCode(resultCode)
                .resultMessage(resultMessage)
                .failureReason(resultMessage)
                .additionalData(additionalData)
                .processedAt(LocalDateTime.now())
                .build();
    }
}