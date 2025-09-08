package com.unicorn.phonebill.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 상품변경 사전체크 응답 DTO
 * API: POST /products/change/validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductChangeValidationResponse {

    @NotNull(message = "성공 여부는 필수입니다")
    private Boolean success;

    @Valid
    private ValidationData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationData {

        @NotNull(message = "검증 결과는 필수입니다")
        private ValidationResult validationResult; // SUCCESS, FAILURE

        private List<ValidationDetail> validationDetails;

        private String failureReason;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class ValidationDetail {

            private CheckType checkType; // PRODUCT_AVAILABLE, OPERATOR_MATCH, LINE_STATUS

            private CheckResult result; // PASS, FAIL

            private String message;
        }
    }

    public enum ValidationResult {
        SUCCESS, FAILURE
    }

    public enum CheckType {
        PRODUCT_AVAILABLE("상품 판매 여부 확인"),
        OPERATOR_MATCH("사업자 일치 확인"),
        LINE_STATUS("회선 상태 확인");

        private final String description;

        CheckType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum CheckResult {
        PASS, FAIL
    }

    /**
     * 성공 응답 생성
     */
    public static ProductChangeValidationResponse success(ValidationData data) {
        return ProductChangeValidationResponse.builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * 실패 응답 생성
     */
    public static ProductChangeValidationResponse failure(String reason, List<ValidationData.ValidationDetail> details) {
        ValidationData data = ValidationData.builder()
                .validationResult(ValidationResult.FAILURE)
                .failureReason(reason)
                .validationDetails(details)
                .build();

        return ProductChangeValidationResponse.builder()
                .success(true)
                .data(data)
                .build();
    }
}