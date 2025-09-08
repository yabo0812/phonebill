package com.unicorn.phonebill.product.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 상품변경 처리 결과 도메인 모델
 */
@Getter
@Builder
public class ProductChangeResult {
    
    private final String requestId;
    private final boolean success;
    private final String resultCode;
    private final String resultMessage;
    private final Product changedProduct;
    private final LocalDateTime processedAt;
    private final Map<String, Object> additionalData;

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
                .additionalData(additionalData)
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 결과가 성공인지 확인
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 결과가 실패인지 확인
     */
    public boolean isFailure() {
        return !success;
    }
}