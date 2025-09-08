package com.phonebill.kosmock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * KOS 상품 변경 요청 DTO
 */
@Data
@Schema(description = "KOS 상품 변경 요청")
public class KosProductChangeRequest {
    
    @Schema(description = "회선번호", example = "01012345678", required = true)
    @NotBlank(message = "회선번호는 필수입니다")
    @Pattern(regexp = "^010\\d{8}$", message = "올바른 회선번호 형식이 아닙니다")
    private String lineNumber;
    
    @Schema(description = "현재 상품 코드", example = "LTE-BASIC-001", required = true)
    @NotBlank(message = "현재 상품 코드는 필수입니다")
    private String currentProductCode;
    
    @Schema(description = "변경할 상품 코드", example = "5G-PREMIUM-001", required = true)
    @NotBlank(message = "변경할 상품 코드는 필수입니다")
    private String targetProductCode;
    
    @Schema(description = "요청 ID", example = "REQ_20250108_002", required = true)
    @NotBlank(message = "요청 ID는 필수입니다")
    private String requestId;
    
    @Schema(description = "요청자 ID", example = "PRODUCT_SERVICE")
    private String requestorId;
    
    @Schema(description = "변경 사유", example = "고객 요청에 의한 상품 변경")
    private String changeReason;
    
    @Schema(description = "적용 일자 (YYYYMMDD)", example = "20250115")
    @Pattern(regexp = "^\\d{8}$", message = "적용 일자는 YYYYMMDD 형식이어야 합니다")
    private String effectiveDate;
}