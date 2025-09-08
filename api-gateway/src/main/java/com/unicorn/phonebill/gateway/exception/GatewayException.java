package com.unicorn.phonebill.gateway.exception;

/**
 * API Gateway 전용 예외 클래스
 * 
 * Gateway에서 발생할 수 있는 다양한 예외 상황을 표현합니다.
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
public class GatewayException extends RuntimeException {
    
    private final String errorCode;
    private final int httpStatus;

    public GatewayException(String message) {
        super(message);
        this.errorCode = "GATEWAY_ERROR";
        this.httpStatus = 500;
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GATEWAY_ERROR";
        this.httpStatus = 500;
    }

    public GatewayException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public GatewayException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}

/**
 * JWT 인증 관련 예외
 */
class JwtAuthenticationException extends GatewayException {
    
    public JwtAuthenticationException(String message) {
        super("JWT_AUTH_ERROR", message, 401);
    }
    
    public JwtAuthenticationException(String message, Throwable cause) {
        super("JWT_AUTH_ERROR", message, 401, cause);
    }
}

/**
 * 서비스 연결 관련 예외
 */
class ServiceConnectionException extends GatewayException {
    
    public ServiceConnectionException(String serviceName, String message) {
        super("SERVICE_CONNECTION_ERROR", 
              String.format("Service '%s' connection failed: %s", serviceName, message), 
              503);
    }
    
    public ServiceConnectionException(String serviceName, String message, Throwable cause) {
        super("SERVICE_CONNECTION_ERROR", 
              String.format("Service '%s' connection failed: %s", serviceName, message), 
              503, cause);
    }
}

/**
 * Rate Limit 관련 예외
 */
class RateLimitExceededException extends GatewayException {
    
    public RateLimitExceededException(String message) {
        super("RATE_LIMIT_EXCEEDED", message, 429);
    }
}

/**
 * 설정 관련 예외
 */
class GatewayConfigurationException extends GatewayException {
    
    public GatewayConfigurationException(String message) {
        super("GATEWAY_CONFIG_ERROR", message, 500);
    }
    
    public GatewayConfigurationException(String message, Throwable cause) {
        super("GATEWAY_CONFIG_ERROR", message, 500, cause);
    }
}