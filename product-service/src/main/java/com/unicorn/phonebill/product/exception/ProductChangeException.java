package com.unicorn.phonebill.product.exception;

/**
 * 상품변경 관련 예외
 */
public class ProductChangeException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ProductChangeException(String errorCode, String message) {
        super(errorCode, message);
    }

    public ProductChangeException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    // 자주 사용되는 예외 팩토리 메소드들
    public static ProductChangeException duplicateRequest(String requestId) {
        return new ProductChangeException("DUPLICATE_REQUEST", 
            "이미 처리 중인 상품변경 요청이 있습니다. RequestId: " + requestId);
    }

    public static ProductChangeException requestNotFound(String requestId) {
        return new ProductChangeException("REQUEST_NOT_FOUND", 
            "상품변경 요청을 찾을 수 없습니다. RequestId: " + requestId);
    }

    public static ProductChangeException invalidStatus(String currentStatus, String expectedStatus) {
        return new ProductChangeException("INVALID_STATUS", 
            String.format("잘못된 상태입니다. 현재: %s, 예상: %s", currentStatus, expectedStatus));
    }

    public static ProductChangeException processingTimeout(String requestId) {
        return new ProductChangeException("PROCESSING_TIMEOUT", 
            "상품변경 처리 시간이 초과되었습니다. RequestId: " + requestId);
    }
}