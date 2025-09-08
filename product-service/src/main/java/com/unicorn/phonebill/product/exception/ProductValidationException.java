package com.unicorn.phonebill.product.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * 상품변경 검증 실패 예외
 */
public class ProductValidationException extends BusinessException {

    private static final long serialVersionUID = 1L;
    
    @SuppressWarnings("serial")
    private final List<String> validationDetails = new ArrayList<>();

    public ProductValidationException(String errorCode, String message, List<String> validationDetails) {
        super(errorCode, message);
        if (validationDetails != null) {
            this.validationDetails.addAll(validationDetails);
        }
    }

    public List<String> getValidationDetails() {
        return validationDetails;
    }

    // 자주 사용되는 검증 예외 팩토리 메소드들
    public static ProductValidationException productNotAvailable(String productCode) {
        return new ProductValidationException("PRODUCT_NOT_AVAILABLE", 
            "판매 중지된 상품입니다: " + productCode,
            List.of("상품코드 " + productCode + "는 현재 판매하지 않는 상품입니다"));
    }

    public static ProductValidationException operatorMismatch(String currentOperator, String targetOperator) {
        return new ProductValidationException("OPERATOR_MISMATCH", 
            "다른 사업자 상품으로는 변경할 수 없습니다",
            List.of(String.format("현재 사업자: %s, 대상 사업자: %s", currentOperator, targetOperator)));
    }

    public static ProductValidationException lineStatusInvalid(String lineStatus) {
        return new ProductValidationException("LINE_STATUS_INVALID", 
            "회선 상태가 올바르지 않습니다: " + lineStatus,
            List.of("정상 상태의 회선만 상품 변경이 가능합니다"));
    }

    public static ProductValidationException sameProductChange(String productCode) {
        return new ProductValidationException("SAME_PRODUCT_CHANGE", 
            "동일한 상품으로는 변경할 수 없습니다",
            List.of("현재 이용 중인 상품과 동일합니다: " + productCode));
    }
}