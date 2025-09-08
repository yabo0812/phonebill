package com.phonebill.bill.exception;

/**
 * 요금조회 관련 비즈니스 예외 클래스
 * 
 * 요금조회 프로세스에서 발생하는 비즈니스 로직 오류를 처리
 * - 유효하지 않은 회선번호
 * - 조회 불가능한 월
 * - 고객 정보 불일치
 * - 요금 데이터 없음
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
public class BillInquiryException extends BusinessException {

    /**
     * 기본 생성자
     * 
     * @param message 오류 메시지
     */
    public BillInquiryException(String message) {
        super("BILL_INQUIRY_ERROR", message);
    }

    /**
     * 상세 정보를 포함한 생성자
     * 
     * @param message 오류 메시지
     * @param detail 상세 오류 정보
     */
    public BillInquiryException(String message, String detail) {
        super("BILL_INQUIRY_ERROR", message, detail);
    }

    /**
     * 원인 예외를 포함한 생성자
     * 
     * @param message 오류 메시지
     * @param cause 원인 예외
     */
    public BillInquiryException(String message, Throwable cause) {
        super("BILL_INQUIRY_ERROR", message, cause);
    }


    // 특정 오류 상황을 위한 정적 팩토리 메소드들

    /**
     * 사용자 정의 오류 코드를 포함한 생성자
     * 
     * @param errorCode 오류 코드
     * @param message 오류 메시지
     * @param detail 상세 정보
     */
    public BillInquiryException(String errorCode, String message, String detail) {
        super(errorCode, message, detail);
    }
    
    /**
     * 유효하지 않은 회선번호 예외
     * 
     * @param lineNumber 회선번호
     * @return BillInquiryException
     */
    public static BillInquiryException invalidLineNumber(String lineNumber) {
        return new BillInquiryException("INVALID_LINE_NUMBER", 
            String.format("유효하지 않은 회선번호: %s", lineNumber), null);
    }
    
    /**
     * 요금 데이터를 찾을 수 없음 예외
     * 
     * @param requestId 요청 ID
     * @param type 데이터 타입
     * @return BillInquiryException
     */
    public static BillInquiryException billDataNotFound(String requestId, String type) {
        return new BillInquiryException("BILL_DATA_NOT_FOUND", 
            String.format("요금 데이터 없음 - %s: %s", type, requestId), null);
    }

    /**
     * 조회 불가능한 월 예외
     * 
     * @param inquiryMonth 조회 월
     * @return BillInquiryException
     */
    public static BillInquiryException invalidInquiryMonth(String inquiryMonth) {
        return new BillInquiryException("INVALID_INQUIRY_MONTH", 
            String.format("조회 불가능한 월: %s", inquiryMonth));
    }

    /**
     * 고객 정보 불일치 예외
     * 
     * @param customerId 고객 ID
     * @param lineNumber 회선번호
     * @return BillInquiryException
     */
    public static BillInquiryException customerMismatch(String customerId, String lineNumber) {
        return new BillInquiryException("CUSTOMER_MISMATCH", 
            String.format("고객 정보 불일치 - 고객ID: %s, 회선번호: %s", customerId, lineNumber));
    }

    /**
     * 요금 데이터 없음 예외
     * 
     * @param lineNumber 회선번호
     * @param inquiryMonth 조회 월
     * @return BillInquiryException
     */
    public static BillInquiryException noBillData(String lineNumber, String inquiryMonth) {
        return new BillInquiryException("NO_BILL_DATA", 
            String.format("요금 데이터 없음 - 회선번호: %s, 조회월: %s", lineNumber, inquiryMonth));
    }

    /**
     * 요금조회 권한 없음 예외
     * 
     * @param customerId 고객 ID
     * @return BillInquiryException
     */
    public static BillInquiryException noPermission(String customerId) {
        return new BillInquiryException("NO_PERMISSION", 
            String.format("요금조회 권한 없음 - 고객ID: %s", customerId));
    }
}