package com.unicorn.phonebill.product.exception;

/**
 * Circuit Breaker 관련 예외
 */
public class CircuitBreakerException extends BusinessException {

    private static final long serialVersionUID = 1L;
    
    private final String serviceName;

    public CircuitBreakerException(String errorCode, String message, String serviceName) {
        super(errorCode, message);
        this.serviceName = serviceName;
    }

    public CircuitBreakerException(String errorCode, String message, String serviceName, Throwable cause) {
        super(errorCode, message, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public static CircuitBreakerException circuitBreakerOpen(String serviceName) {
        return new CircuitBreakerException("CIRCUIT_BREAKER_OPEN", 
            "Circuit Breaker가 OPEN 상태입니다. 잠시 후 다시 시도해주세요", serviceName);
    }

    public static CircuitBreakerException circuitBreakerTimeout(String serviceName) {
        return new CircuitBreakerException("CIRCUIT_BREAKER_TIMEOUT", 
            "Circuit Breaker 타임아웃이 발생했습니다", serviceName);
    }
}