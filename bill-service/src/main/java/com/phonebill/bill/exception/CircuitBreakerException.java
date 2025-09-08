package com.phonebill.bill.exception;

/**
 * Circuit Breaker Open 상태 예외 클래스
 * 
 * Circuit Breaker가 Open 상태일 때 발생하는 예외
 * 외부 시스템의 장애나 응답 지연으로 인해 요청이 차단되는 상황을 처리
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
public class CircuitBreakerException extends BusinessException {

    /**
     * 서비스명
     */
    private final String serviceName;

    /**
     * Circuit Breaker 상태 정보
     */
    private final String stateInfo;

    /**
     * 기본 생성자
     * 
     * @param serviceName 서비스명
     * @param message 오류 메시지
     */
    public CircuitBreakerException(String serviceName, String message) {
        super("CIRCUIT_BREAKER_OPEN", message);
        this.serviceName = serviceName;
        this.stateInfo = null;
    }

    /**
     * 상태 정보를 포함한 생성자
     * 
     * @param serviceName 서비스명
     * @param message 오류 메시지
     * @param stateInfo 상태 정보
     */
    public CircuitBreakerException(String serviceName, String message, String stateInfo) {
        super("CIRCUIT_BREAKER_OPEN", message, stateInfo);
        this.serviceName = serviceName;
        this.stateInfo = stateInfo;
    }

    /**
     * 원인 예외를 포함한 생성자
     * 
     * @param serviceName 서비스명
     * @param message 오류 메시지
     * @param cause 원인 예외
     */
    public CircuitBreakerException(String serviceName, String message, Throwable cause) {
        super("CIRCUIT_BREAKER_OPEN", message, cause);
        this.serviceName = serviceName;
        this.stateInfo = null;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getStateInfo() {
        return stateInfo;
    }

    // 특정 오류 상황을 위한 정적 팩토리 메소드들

    /**
     * Circuit Breaker Open 상태 예외
     * 
     * @param serviceName 서비스명
     * @return 예외 인스턴스
     */
    public static CircuitBreakerException circuitBreakerOpen(String serviceName) {
        return new CircuitBreakerException(
            serviceName,
            "일시적으로 서비스 이용이 어렵습니다",
            String.format("%s 서비스가 일시적으로 중단되었습니다. 잠시 후 다시 시도해주세요.", serviceName)
        );
    }

    /**
     * Circuit Breaker Open 상태 예외 (상세 정보 포함)
     * 
     * @param serviceName 서비스명
     * @param failureRate 실패율
     * @param slowCallRate 느린 호출 비율
     * @return 예외 인스턴스
     */
    public static CircuitBreakerException circuitBreakerOpenWithDetails(String serviceName, double failureRate, double slowCallRate) {
        return new CircuitBreakerException(
            serviceName,
            "서비스 품질 저하로 인해 일시적으로 차단되었습니다",
            String.format("서비스: %s, 실패율: %.2f%%, 느린 호출 비율: %.2f%%", serviceName, failureRate * 100, slowCallRate * 100)
        );
    }

    /**
     * Circuit Breaker Half-Open 상태에서 호출 차단 예외
     * 
     * @param serviceName 서비스명
     * @return 예외 인스턴스
     */
    public static CircuitBreakerException callNotPermittedInHalfOpenState(String serviceName) {
        return new CircuitBreakerException(
            serviceName,
            "서비스 상태 확인 중입니다",
            String.format("%s 서비스의 상태를 확인하는 중이므로 잠시 후 다시 시도해주세요.", serviceName)
        );
    }

    /**
     * Circuit Breaker 설정 오류 예외
     * 
     * @param serviceName 서비스명
     * @param configError 설정 오류 내용
     * @return 예외 인스턴스
     */
    public static CircuitBreakerException configurationError(String serviceName, String configError) {
        return new CircuitBreakerException(
            serviceName,
            "Circuit Breaker 설정에 문제가 있습니다",
            String.format("서비스: %s, 설정 오류: %s", serviceName, configError)
        );
    }
}