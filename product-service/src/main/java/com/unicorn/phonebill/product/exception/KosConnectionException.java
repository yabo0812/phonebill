package com.unicorn.phonebill.product.exception;

/**
 * KOS 연동 관련 예외
 */
public class KosConnectionException extends BusinessException {

    private static final long serialVersionUID = 1L;
    
    private final String serviceName;

    public KosConnectionException(String errorCode, String message, String serviceName) {
        super(errorCode, message);
        this.serviceName = serviceName;
    }

    public KosConnectionException(String errorCode, String message, String serviceName, Throwable cause) {
        super(errorCode, message, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    // 자주 사용되는 KOS 연동 예외 팩토리 메소드들
    public static KosConnectionException connectionTimeout(String serviceName) {
        return new KosConnectionException("KOS_CONNECTION_TIMEOUT", 
            "KOS 시스템 연결 시간이 초과되었습니다", serviceName);
    }

    public static KosConnectionException serviceUnavailable(String serviceName) {
        return new KosConnectionException("KOS_SERVICE_UNAVAILABLE", 
            "KOS 시스템에 접근할 수 없습니다", serviceName);
    }

    public static KosConnectionException invalidResponse(String serviceName, String details) {
        return new KosConnectionException("KOS_INVALID_RESPONSE", 
            "KOS 시스템에서 잘못된 응답을 받았습니다: " + details, serviceName);
    }

    public static KosConnectionException authenticationFailed(String serviceName) {
        return new KosConnectionException("KOS_AUTH_FAILED", 
            "KOS 시스템 인증에 실패했습니다", serviceName);
    }
}