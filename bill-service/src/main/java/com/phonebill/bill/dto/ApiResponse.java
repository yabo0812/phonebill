package com.phonebill.bill.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 응답 공통 포맷 클래스
 * 
 * 모든 API 응답에 대한 공통 구조를 제공
 * - success: 성공/실패 여부
 * - resultCode: 결과 코드
 * - resultMessage: 결과 메시지
 * - data: 실제 응답 데이터 (성공시)
 * - error: 오류 정보 (실패시)  
 * - timestamp: 응답 시간
 * - traceId: 추적 ID
 * 
 * @param <T> 응답 데이터 타입
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * 성공/실패 여부
     */
    private boolean success;

    /**
     * 결과 코드
     */
    private String resultCode;

    /**
     * 결과 메시지
     */
    private String resultMessage;

    /**
     * 응답 데이터 (성공시에만 포함)
     */
    private T data;

    /**
     * 오류 정보 (실패시에만 포함)
     */
    private ErrorDetail error;

    /**
     * 응답 시간
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 추적 ID
     */
    private String traceId;

    /**
     * 성공 응답 생성
     * 
     * @param data 응답 데이터
     * @param message 성공 메시지
     * @param <T> 데이터 타입
     * @return 성공 응답
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .resultCode("0000")
                .resultMessage(message)
                .data(data)
                .build();
    }

    /**
     * 성공 응답 생성 (기본 메시지)
     * 
     * @param data 응답 데이터
     * @param <T> 데이터 타입
     * @return 성공 응답
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "요청이 성공적으로 처리되었습니다");
    }

    /**
     * 실패 응답 생성
     * 
     * @param error 오류 정보
     * @param message 오류 메시지
     * @return 실패 응답
     */
    public static ApiResponse<Void> failure(ErrorDetail error, String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .resultCode(error.getCode())
                .resultMessage(message)
                .error(error)
                .build();
    }

    /**
     * 실패 응답 생성 (단순 오류)
     * 
     * @param code 오류 코드
     * @param message 오류 메시지
     * @return 실패 응답
     */
    public static ApiResponse<Void> failure(String code, String message) {
        ErrorDetail error = ErrorDetail.builder()
                .code(code)
                .message(message)
                .build();
        return ApiResponse.<Void>builder()
                .success(false)
                .resultCode(code)
                .resultMessage(message)
                .error(error)
                .build();
    }
}

/**
 * 오류 상세 정보 클래스
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class ErrorDetail {

    /**
     * 오류 코드
     */
    private String code;

    /**
     * 오류 메시지
     */
    private String message;

    /**
     * 상세 오류 정보
     */
    private String detail;

    /**
     * 오류 발생 시간
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}