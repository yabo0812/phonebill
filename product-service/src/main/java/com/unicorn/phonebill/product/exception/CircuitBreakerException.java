package com.unicorn.phonebill.product.exception;

/**
 * Circuit Breaker Open 상태 예외
 */
public class CircuitBreakerException extends BusinessException {

    private static final long serialVersionUID = 1L;
    
    private final String serviceName;
    private final String circuitBreakerState;

    public CircuitBreakerException(String errorCode, String message, String serviceName, String circuitBreakerState) {
        super(errorCode, message);
        this.serviceName = serviceName;
        this.circuitBreakerState = circuitBreakerState;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getCircuitBreakerState() {
        return circuitBreakerState;
    }

    // 자주 사용되는 Circuit Breaker 예외 팩토리 메소드들
    public static CircuitBreakerException circuitOpen(String serviceName) {
        return new CircuitBreakerException("CIRCUIT_BREAKER_OPEN", 
            "서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.", 
            serviceName, "OPEN");
    }

    public static CircuitBreakerException halfOpenFailed(String serviceName) {
        return new CircuitBreakerException("CIRCUIT_BREAKER_HALF_OPEN_FAILED", 
            "서비스 복구 시도 중 실패했습니다", 
            serviceName, "HALF_OPEN");
    }

    public static CircuitBreakerException callNotPermitted(String serviceName) {
        return new CircuitBreakerException("CIRCUIT_BREAKER_CALL_NOT_PERMITTED", 
            "서비스 호출이 차단되었습니다", 
            serviceName, "OPEN");
    }
}