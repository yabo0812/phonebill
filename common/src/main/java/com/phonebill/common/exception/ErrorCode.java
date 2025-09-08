package com.phonebill.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 오류 코드 열거형
 * 시스템 전체에서 사용되는 표준화된 오류 코드를 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // 공통 오류
    INTERNAL_SERVER_ERROR("E0001", "내부 서버 오류가 발생했습니다."),
    INVALID_INPUT_VALUE("E0002", "입력값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED("E0003", "허용되지 않은 HTTP 메소드입니다."),
    ENTITY_NOT_FOUND("E0004", "요청한 리소스를 찾을 수 없습니다."),
    INVALID_TYPE_VALUE("E0005", "잘못된 타입의 값입니다."),
    HANDLE_ACCESS_DENIED("E0006", "접근이 거부되었습니다."),
    
    // 인증/인가 오류
    UNAUTHORIZED("E1001", "인증이 필요합니다."),
    FORBIDDEN("E1002", "권한이 없습니다."),
    INVALID_TOKEN("E1003", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED("E1004", "토큰이 만료되었습니다."),
    LOGIN_REQUIRED("E1005", "로그인이 필요합니다."),
    ACCOUNT_LOCKED("E1006", "계정이 잠겨있습니다."),
    INVALID_CREDENTIALS("E1007", "잘못된 인증 정보입니다."),
    
    // 비즈니스 오류
    BUSINESS_ERROR("E2001", "비즈니스 로직 오류가 발생했습니다."),
    VALIDATION_ERROR("E2002", "검증 오류가 발생했습니다."),
    DUPLICATE_RESOURCE("E2003", "중복된 리소스입니다."),
    RESOURCE_NOT_FOUND("E2004", "요청한 리소스를 찾을 수 없습니다."),
    OPERATION_NOT_ALLOWED("E2005", "허용되지 않은 작업입니다."),
    
    // 외부 시스템 연동 오류
    EXTERNAL_SYSTEM_ERROR("E3001", "외부 시스템 연동 오류가 발생했습니다."),
    CIRCUIT_BREAKER_OPEN("E3002", "외부 시스템이 일시적으로 사용할 수 없습니다."),
    TIMEOUT_ERROR("E3003", "요청 시간이 초과되었습니다."),
    CONNECTION_ERROR("E3004", "연결 오류가 발생했습니다."),
    
    // 데이터베이스 오류
    DATABASE_ERROR("E4001", "데이터베이스 오류가 발생했습니다."),
    CONSTRAINT_VIOLATION("E4002", "데이터 제약 조건 위반이 발생했습니다."),
    TRANSACTION_ERROR("E4003", "트랜잭션 오류가 발생했습니다."),
    
    // 캐시 오류
    CACHE_ERROR("E5001", "캐시 오류가 발생했습니다."),
    CACHE_NOT_FOUND("E5002", "캐시에서 데이터를 찾을 수 없습니다."),
    
    // 요금조회 관련 오류
    BILL_INQUIRY_ERROR("E6001", "요금조회 중 오류가 발생했습니다."),
    BILL_NOT_FOUND("E6002", "요금 정보를 찾을 수 없습니다."),
    BILL_INQUIRY_FAILED("E6003", "요금조회에 실패했습니다."),
    
    // 상품변경 관련 오류
    PRODUCT_CHANGE_ERROR("E7001", "상품변경 중 오류가 발생했습니다."),
    PRODUCT_NOT_FOUND("E7002", "상품 정보를 찾을 수 없습니다."),
    PRODUCT_VALIDATION_ERROR("E7003", "상품변경 검증에 실패했습니다."),
    PRODUCT_CHANGE_FAILED("E7004", "상품변경에 실패했습니다."),
    
    // KOS 연동 오류
    KOS_CONNECTION_ERROR("E8001", "KOS 시스템 연결 오류가 발생했습니다."),
    KOS_RESPONSE_ERROR("E8002", "KOS 시스템 응답 오류가 발생했습니다."),
    KOS_TIMEOUT_ERROR("E8003", "KOS 시스템 응답 시간 초과가 발생했습니다."),
    KOS_SERVICE_UNAVAILABLE("E8004", "KOS 시스템이 일시적으로 사용할 수 없습니다.");
    
    private final String code;
    private final String message;
}
