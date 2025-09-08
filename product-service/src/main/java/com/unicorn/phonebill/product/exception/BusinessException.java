package com.unicorn.phonebill.product.exception;

/**
 * 비즈니스 예외 기본 클래스
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    private final String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}