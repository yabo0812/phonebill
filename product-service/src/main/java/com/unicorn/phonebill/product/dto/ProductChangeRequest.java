package com.unicorn.phonebill.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 상품변경 요청 DTO
 * API: POST /products/change
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductChangeRequest {

    @JsonProperty("lineNumber")
    @NotBlank(message = "회선번호는 필수입니다")
    private String lineNumber;

    @JsonProperty("currentProductCode")
    @NotBlank(message = "현재 상품 코드는 필수입니다")
    private String currentProductCode;

    @JsonProperty("targetProductCode")
    @NotBlank(message = "변경 대상 상품 코드는 필수입니다")
    private String targetProductCode;
    
    /**
     * 회선번호 설정 시 대시 제거 및 유효성 검증
     */
    public void setLineNumber(String lineNumber) {
        if (lineNumber != null) {
            // 대시 제거
            String normalized = lineNumber.replaceAll("-", "");
            
            // 유효성 검증
            if (!normalized.matches("^010[0-9]{8}$")) {
                throw new IllegalArgumentException("회선번호는 010으로 시작하는 11자리 숫자여야 합니다");
            }
            
            this.lineNumber = normalized;
        } else {
            this.lineNumber = null;
        }
    }
}