package com.phonebill.kosmock.exception;

/**
 * KOS Mock 서비스 전용 예외
 */
public class KosMockException extends RuntimeException {
    
    private final String errorCode;
    
    public KosMockException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public KosMockException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}