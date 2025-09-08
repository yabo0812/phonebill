package com.phonebill.common.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 처리 중 발생하는 예외
 * 일반적인 업무 처리 과정에서 예상되는 오류 상황을 나타냄
 */
@Getter
public class BusinessException extends RuntimeException {
    
    /**
     * 오류 코드
     */
    private final String errorCode;
    
    /**
     * HTTP 상태 코드
     */
    private final int httpStatus;
    
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
        this.httpStatus = 400; // Bad Request
    }
    
    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 400; // Bad Request
    }
    
    public BusinessException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public BusinessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = 400; // Bad Request
    }
    
    public BusinessException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}