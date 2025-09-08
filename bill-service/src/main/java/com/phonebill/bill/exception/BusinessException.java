package com.phonebill.bill.exception;

/**
 * 비즈니스 로직 예외를 위한 기본 예외 클래스
 * 
 * 애플리케이션의 비즈니스 규칙 위반이나 예상 가능한 오류 상황을 표현
 * 모든 비즈니스 예외의 부모 클래스로 사용
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
public abstract class BusinessException extends RuntimeException {

    /**
     * 오류 코드
     */
    private final String errorCode;

    /**
     * 상세 오류 정보
     */
    private final String detail;

    /**
     * 생성자
     * 
     * @param errorCode 오류 코드
     * @param message 오류 메시지
     */
    protected BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.detail = null;
    }

    /**
     * 생성자 (상세 정보 포함)
     * 
     * @param errorCode 오류 코드
     * @param message 오류 메시지
     * @param detail 상세 오류 정보
     */
    protected BusinessException(String errorCode, String message, String detail) {
        super(message);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    /**
     * 생성자 (원인 예외 포함)
     * 
     * @param errorCode 오류 코드
     * @param message 오류 메시지
     * @param cause 원인 예외
     */
    protected BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.detail = null;
    }

    /**
     * 생성자 (모든 정보 포함)
     * 
     * @param errorCode 오류 코드
     * @param message 오류 메시지
     * @param detail 상세 오류 정보
     * @param cause 원인 예외
     */
    protected BusinessException(String errorCode, String message, String detail, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }
}