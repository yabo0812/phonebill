package com.phonebill.kosmock.data;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Mock 처리 결과 데이터 모델
 * KOS 시스템의 비동기 처리 결과를 모방합니다.
 */
@Data
@Builder
public class MockProcessingResult {
    
    /**
     * 요청 ID
     */
    private String requestId;
    
    /**
     * 처리 유형 (BILL_INQUIRY, PRODUCT_CHANGE)
     */
    private String processingType;
    
    /**
     * 처리 상태 (PROCESSING, SUCCESS, FAILURE)
     */
    private String status;
    
    /**
     * 처리 결과 메시지
     */
    private String message;
    
    /**
     * 처리 결과 데이터 (JSON String)
     */
    private String resultData;
    
    /**
     * 요청 일시
     */
    private LocalDateTime requestedAt;
    
    /**
     * 처리 완료 일시
     */
    private LocalDateTime completedAt;
    
    /**
     * 오류 코드 (실패 시)
     */
    private String errorCode;
    
    /**
     * 오류 상세 메시지 (실패 시)
     */
    private String errorDetails;
    
    /**
     * 재시도 횟수
     */
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * 처리 소요 시간 (밀리초)
     */
    private Long processingTimeMs;
}