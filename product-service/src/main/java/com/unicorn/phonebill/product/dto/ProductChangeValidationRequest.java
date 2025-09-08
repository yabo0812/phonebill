package com.unicorn.phonebill.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 상품변경 사전체크 요청 DTO
 * API: POST /products/change/validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductChangeValidationRequest {

    @NotBlank(message = "회선번호는 필수입니다")
    @Pattern(regexp = "^010[0-9]{8}$", message = "회선번호는 010으로 시작하는 11자리 숫자여야 합니다")
    private String lineNumber;

    @NotBlank(message = "현재 상품 코드는 필수입니다")
    private String currentProductCode;

    @NotBlank(message = "변경 대상 상품 코드는 필수입니다")
    private String targetProductCode;
}