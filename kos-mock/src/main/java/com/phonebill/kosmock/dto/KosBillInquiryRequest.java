package com.phonebill.kosmock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * KOS 요금 조회 요청 DTO
 */
@Data
@Schema(description = "KOS 요금 조회 요청")
public class KosBillInquiryRequest {
    
    @Schema(description = "회선번호", example = "01012345679", required = true)
    @NotBlank(message = "회선번호는 필수입니다")
    @Pattern(regexp = "^010\\d{8}$", message = "올바른 회선번호 형식이 아닙니다")
    private String lineNumber;
    
    @Schema(description = "청구월 (YYYYMM)", example = "202508")
    @Pattern(regexp = "^\\d{6}$", message = "청구월은 YYYYMM 형식이어야 합니다")
    private String billingMonth;
    
    @Schema(description = "요청 ID", example = "REQ_20250108_001", required = true)
    @NotBlank(message = "요청 ID는 필수입니다")
    private String requestId;
}