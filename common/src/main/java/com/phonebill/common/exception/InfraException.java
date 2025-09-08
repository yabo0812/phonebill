package com.phonebill.common.exception;

/**
 * 인프라 예외
 * 데이터베이스, 캐시, 외부 시스템 연동 등 인프라 관련 오류를 나타냅니다.
 */
public class InfraException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public InfraException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public InfraException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public InfraException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
    
    public InfraException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
