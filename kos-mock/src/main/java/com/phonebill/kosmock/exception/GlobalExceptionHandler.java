package com.phonebill.kosmock.exception;

import com.phonebill.kosmock.dto.KosCommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Bean Validation 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<KosCommonResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
            
        log.warn("입력값 검증 실패: {}", errorMessage);
        
        return ResponseEntity.badRequest()
            .body(KosCommonResponse.failure("9001", "입력값이 올바르지 않습니다: " + errorMessage));
    }

    /**
     * Bean Binding 실패 처리
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<KosCommonResponse<Object>> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
            
        log.warn("데이터 바인딩 실패: {}", errorMessage);
        
        return ResponseEntity.badRequest()
            .body(KosCommonResponse.failure("9002", "데이터 바인딩에 실패했습니다: " + errorMessage));
    }

    /**
     * HTTP 메시지 읽기 실패 처리
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<KosCommonResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("HTTP 메시지 읽기 실패", e);
        
        return ResponseEntity.badRequest()
            .body(KosCommonResponse.failure("9003", "요청 데이터 형식이 올바르지 않습니다"));
    }

    /**
     * 메서드 인자 타입 불일치 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<KosCommonResponse<Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("메서드 인자 타입 불일치: {}", e.getMessage());
        
        return ResponseEntity.badRequest()
            .body(KosCommonResponse.failure("9004", "요청 파라미터 타입이 올바르지 않습니다"));
    }

    /**
     * 지원하지 않는 HTTP 메서드 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<KosCommonResponse<Object>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("지원하지 않는 HTTP 메서드: {}", e.getMethod());
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(KosCommonResponse.failure("9005", "지원하지 않는 HTTP 메서드입니다"));
    }

    /**
     * 핸들러를 찾을 수 없음 처리
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<KosCommonResponse<Object>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("핸들러를 찾을 수 없음: {}", e.getRequestURL());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(KosCommonResponse.failure("9006", "요청한 API를 찾을 수 없습니다"));
    }

    /**
     * KOS Mock 특화 예외 처리
     */
    @ExceptionHandler(KosMockException.class)
    public ResponseEntity<KosCommonResponse<Object>> handleKosMockException(KosMockException e) {
        log.warn("KOS Mock 예외 발생: {}", e.getMessage());
        
        return ResponseEntity.ok()
            .body(KosCommonResponse.failure(e.getErrorCode(), e.getMessage()));
    }

    /**
     * 런타임 예외 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<KosCommonResponse<Object>> handleRuntimeException(RuntimeException e) {
        log.error("런타임 예외 발생", e);
        
        // Mock 환경에서는 특정 에러 메시지들을 그대로 반환
        if (e.getMessage() != null && e.getMessage().contains("KOS 시스템")) {
            return ResponseEntity.ok()
                .body(KosCommonResponse.failure("8888", e.getMessage()));
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(KosCommonResponse.failure("9998", "처리 중 오류가 발생했습니다"));
    }

    /**
     * 모든 예외 처리 (최종 catch)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<KosCommonResponse<Object>> handleException(Exception e) {
        log.error("예상하지 못한 예외 발생", e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(KosCommonResponse.failure("9999", "시스템 오류가 발생했습니다"));
    }
}