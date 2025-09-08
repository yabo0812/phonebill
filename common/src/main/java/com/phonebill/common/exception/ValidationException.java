package com.phonebill.common.exception;

/**
 * 데이터 검증 실패시 발생하는 예외
 * 입력 데이터가 비즈니스 규칙에 맞지 않을 때 사용
 */
public class ValidationException extends BusinessException {
    
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", 400);
    }
    
    public ValidationException(String field, String message) {
        super(String.format("%s: %s", field, message), "VALIDATION_ERROR", 400);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, "VALIDATION_ERROR", 400, cause);
    }
}