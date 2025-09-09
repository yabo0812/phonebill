package com.unicorn.phonebill.product.dto.kos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * KOS 공통 응답 DTO
 */
@Data
@Builder
@Schema(description = "KOS 공통 응답")
public class KosCommonResponse<T> {
    
    @Schema(description = "성공 여부", example = "true")
    private Boolean success;
    
    @Schema(description = "처리 결과 코드", example = "0000")
    private String resultCode;
    
    @Schema(description = "처리 결과 메시지", example = "정상 처리되었습니다")
    private String resultMessage;
    
    @Schema(description = "응답 데이터")
    private T data;
    
    @Schema(description = "처리 시간", example = "2025-01-08T14:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "요청 추적 ID", example = "TRACE_20250108_001")
    private String traceId;
    
    /**
     * 성공 응답 생성
     */
    public static <T> KosCommonResponse<T> success(T data) {
        return KosCommonResponse.<T>builder()
                .success(true)
                .resultCode("0000")
                .resultMessage("정상 처리되었습니다")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 성공 응답 생성 (메시지 포함)
     */
    public static <T> KosCommonResponse<T> success(T data, String message) {
        return KosCommonResponse.<T>builder()
                .success(true)
                .resultCode("0000")
                .resultMessage(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 실패 응답 생성
     */
    public static <T> KosCommonResponse<T> failure(String errorCode, String errorMessage) {
        return KosCommonResponse.<T>builder()
                .success(false)
                .resultCode(errorCode)
                .resultMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 시스템 오류 응답 생성
     */
    public static <T> KosCommonResponse<T> systemError() {
        return KosCommonResponse.<T>builder()
                .success(false)
                .resultCode("9999")
                .resultMessage("시스템 오류가 발생했습니다")
                .timestamp(LocalDateTime.now())
                .build();
    }
}