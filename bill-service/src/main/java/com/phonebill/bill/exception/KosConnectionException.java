package com.phonebill.bill.exception;

/**
 * KOS 시스템 연동 관련 예외 클래스
 * 
 * KOS(통신사 백엔드 시스템)와의 연동에서 발생하는 오류를 처리
 * - 네트워크 연결 실패
 * - 응답 시간 초과
 * - KOS API 오류 응답
 * - 데이터 변환 오류
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
public class KosConnectionException extends BusinessException {

    /**
     * 연동 서비스명
     */
    private final String serviceName;

    /**
     * 기본 생성자
     * 
     * @param serviceName 연동 서비스명
     * @param message 오류 메시지
     */
    public KosConnectionException(String serviceName, String message) {
        super("KOS_CONNECTION_ERROR", message);
        this.serviceName = serviceName;
    }

    /**
     * 상세 정보를 포함한 생성자
     * 
     * @param serviceName 연동 서비스명
     * @param message 오류 메시지
     * @param detail 상세 오류 정보
     */
    public KosConnectionException(String serviceName, String message, String detail) {
        super("KOS_CONNECTION_ERROR", message, detail);
        this.serviceName = serviceName;
    }

    /**
     * 원인 예외를 포함한 생성자
     * 
     * @param serviceName 연동 서비스명
     * @param message 오류 메시지
     * @param cause 원인 예외
     */
    public KosConnectionException(String serviceName, String message, Throwable cause) {
        super("KOS_CONNECTION_ERROR", message, cause);
        this.serviceName = serviceName;
    }


    public String getServiceName() {
        return serviceName;
    }

    // 특정 오류 상황을 위한 정적 팩토리 메소드들

    /**
     * 연결 시간 초과 예외
     * 
     * @param serviceName 서비스명
     * @param timeout 시간 초과 값(초)
     * @return KosConnectionException
     */
    public static KosConnectionException timeout(String serviceName, int timeout) {
        return new KosConnectionException(serviceName, 
            String.format("KOS 연결 시간 초과 (%d초)", timeout));
    }

    /**
     * 네트워크 연결 실패 예외
     * 
     * @param serviceName 서비스명
     * @param host 호스트명
     * @param port 포트번호
     * @return KosConnectionException
     */
    public static KosConnectionException connectionFailed(String serviceName, String host, int port) {
        return new KosConnectionException(serviceName, 
            String.format("KOS 연결 실패 - %s:%d", host, port));
    }

    /**
     * KOS API 오류 응답 예외
     * 
     * @param serviceName 서비스명
     * @param errorCode KOS 오류 코드
     * @param errorMessage KOS 오류 메시지
     * @return KosConnectionException
     */
    public static KosConnectionException apiError(String serviceName, String errorCode, String errorMessage) {
        return new KosConnectionException(serviceName, errorCode, 
            String.format("KOS API 오류 - 코드: %s, 메시지: %s", errorCode, errorMessage));
    }

    /**
     * 데이터 변환 오류 예외
     * 
     * @param serviceName 서비스명
     * @param dataType 데이터 타입
     * @param cause 원인 예외
     * @return KosConnectionException
     */
    public static KosConnectionException dataConversionError(String serviceName, String dataType, Throwable cause) {
        return new KosConnectionException(serviceName, 
            String.format("KOS 데이터 변환 오류 - 타입: %s", dataType), cause);
    }
    
    /**
     * 네트워크 오류 예외
     * 
     * @param serviceName 서비스명
     * @param cause 원인 예외
     * @return KosConnectionException
     */
    public static KosConnectionException networkError(String serviceName, Throwable cause) {
        return new KosConnectionException(serviceName, 
            "KOS 네트워크 연결 오류", cause);
    }
}