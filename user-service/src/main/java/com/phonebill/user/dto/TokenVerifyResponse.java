package com.phonebill.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 토큰 검증 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVerifyResponse {
    
    private boolean valid;
    private String userId;
    private String customerId;
    private String lineNumber;
    private LocalDateTime expiresAt;
    private String message;
    
    /**
     * 유효한 토큰에 대한 응답 생성
     */
    public static TokenVerifyResponse valid(String userId, String customerId, String lineNumber, LocalDateTime expiresAt) {
        return TokenVerifyResponse.builder()
                .valid(true)
                .userId(userId)
                .customerId(customerId)
                .lineNumber(lineNumber)
                .expiresAt(expiresAt)
                .message("유효한 토큰입니다.")
                .build();
    }
    
    /**
     * 유효하지 않은 토큰에 대한 응답 생성
     */
    public static TokenVerifyResponse invalid() {
        return TokenVerifyResponse.builder()
                .valid(false)
                .message("유효하지 않은 토큰입니다.")
                .build();
    }
    
    /**
     * 만료된 토큰에 대한 응답 생성
     */
    public static TokenVerifyResponse expired() {
        return TokenVerifyResponse.builder()
                .valid(false)
                .message("만료된 토큰입니다.")
                .build();
    }
}