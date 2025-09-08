package com.phonebill.bill.exception;

import com.phonebill.bill.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 * 
 * 애플리케이션에서 발생하는 모든 예외를 일관된 형태로 처리
 * - 비즈니스 예외: 예상 가능한 오류 상황
 * - 시스템 예외: 예상치 못한 시스템 오류
 * - 검증 예외: 입력값 검증 실패
 * - HTTP 예외: HTTP 프로토콜 관련 오류
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 요금조회 관련 비즈니스 예외 처리
     */
    @ExceptionHandler(BillInquiryException.class)
    public ResponseEntity<ApiResponse<Void>> handleBillInquiryException(
            BillInquiryException ex, HttpServletRequest request) {
        log.warn("요금조회 비즈니스 예외 발생: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * KOS 연동 예외 처리
     */
    @ExceptionHandler(KosConnectionException.class)
    public ResponseEntity<ApiResponse<Void>> handleKosConnectionException(
            KosConnectionException ex, HttpServletRequest request) {
        log.error("KOS 연동 오류 발생: {} - {}, 서비스: {}", 
                ex.getErrorCode(), ex.getMessage(), ex.getServiceName());
        
        // KOS 연동 오류는 503 Service Unavailable로 응답
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * Circuit Breaker 예외 처리
     */
    @ExceptionHandler(CircuitBreakerException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitBreakerException(
            CircuitBreakerException ex, HttpServletRequest request) {
        log.warn("Circuit Breaker 예외 발생: {} - {}, 서비스: {}", 
                ex.getErrorCode(), ex.getMessage(), ex.getServiceName());
        
        // Circuit Breaker 오류는 503 Service Unavailable로 응답
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * 일반 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("비즈니스 예외 발생: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * Bean Validation 예외 처리 (@Valid 어노테이션)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        log.warn("입력값 검증 실패: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .data(errors)
                        .message("입력값이 올바르지 않습니다")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * Bean Validation 예외 처리 (@ModelAttribute)
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBindException(BindException ex) {
        log.warn("바인딩 검증 실패: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .data(errors)
                        .message("입력값이 올바르지 않습니다")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * Constraint Validation 예외 처리 (경로 변수, 요청 파라미터)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        log.warn("제약조건 검증 실패: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            errors.put(fieldName, violation.getMessage());
        }
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .data(errors)
                        .message("입력값이 올바르지 않습니다")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * 필수 요청 파라미터 누락 예외 처리
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameterException(
            MissingServletRequestParameterException ex) {
        log.warn("필수 파라미터 누락: {}", ex.getMessage());
        
        String message = String.format("필수 파라미터가 누락되었습니다: %s", ex.getParameterName());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("MISSING_PARAMETER", message));
    }

    /**
     * 타입 불일치 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        log.warn("파라미터 타입 불일치: {}", ex.getMessage());
        
        String message = String.format("파라미터 '%s'의 값이 올바르지 않습니다", ex.getName());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("INVALID_PARAMETER_TYPE", message));
    }

    /**
     * HTTP 메소드 지원하지 않음 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex) {
        log.warn("지원하지 않는 HTTP 메소드: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.failure("METHOD_NOT_ALLOWED", 
                        "지원하지 않는 HTTP 메소드입니다"));
    }

    /**
     * JSON 파싱 오류 예외 처리
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        log.warn("JSON 파싱 오류: {}", ex.getMessage());
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("INVALID_JSON_FORMAT", 
                        "요청 데이터 형식이 올바르지 않습니다"));
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("예상치 못한 시스템 오류 발생: ", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("INTERNAL_SERVER_ERROR", 
                        "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요"));
    }
}