package com.phonebill.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 오류 응답 구조
 * API 오류 발생 시 표준화된 응답 형식을 제공합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private String code;
    private String message;
    private String detail;
    private LocalDateTime timestamp;
    private String path;
    
    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse of(String code, String message, String detail) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .detail(detail)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse of(String code, String message, String detail, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .detail(detail)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}
