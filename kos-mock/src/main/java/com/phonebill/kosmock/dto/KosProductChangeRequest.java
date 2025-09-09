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
    
    @Schema(description = "회선번호", example = "01012345679", required = true)
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
}