package com.phonebill.bill.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 요금조회 요청 DTO
 * 
 * 요금조회 API 요청에 필요한 데이터를 담는 객체
 * - 회선번호 (필수): 조회할 대상 회선
 * - 조회월 (선택): 특정월 조회시 사용, 미입력시 당월 조회
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillInquiryRequest {

    /**
     * 조회할 회선번호 (필수)
     * 010-XXXX-XXXX 또는 01XXXXXXXXX 형식 허용
     */
    @JsonProperty("lineNumber")
    @NotBlank(message = "회선번호는 필수입니다")
    private String lineNumber;

    /**
     * 조회월 (선택)
     * YYYYMM 형식, 미입력시 당월 조회
     */
    @JsonProperty("inquiryMonth")
    @Pattern(
        regexp = "^\\d{6}$", 
        message = "조회월은 YYYYMM 형식이어야 합니다"
    )
    private String inquiryMonth;
    
    /**
     * 회선번호 설정 시 정규화 처리
     * - 대시가 있는 경우: 010-1234-5678 → 01012345678
     * - 대시가 없는 경우: 01012345678 → 그대로 유지
     * - 유효성 검증: 010으로 시작하는 11자리 숫자
     */
    public void setLineNumber(String lineNumber) {
        if (lineNumber != null) {
            // 대시 제거하여 정규화
            String normalized = lineNumber.replaceAll("-", "");
            
            // 유효성 검증: 010으로 시작하는 11자리 숫자
            if (!normalized.matches("^010\\d{8}$")) {
                throw new IllegalArgumentException("회선번호는 010으로 시작하는 11자리 숫자이거나 010-XXXX-XXXX 형식이어야 합니다");
            }
            
            this.lineNumber = normalized;
        } else {
            this.lineNumber = null;
        }
    }
}