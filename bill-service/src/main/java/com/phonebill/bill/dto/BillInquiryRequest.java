package com.phonebill.bill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillInquiryRequest {

    /**
     * 조회할 회선번호 (필수)
     * 010-XXXX-XXXX 형식만 허용
     */
    @NotBlank(message = "회선번호는 필수입니다")
    @Pattern(
        regexp = "^010-\\d{4}-\\d{4}$", 
        message = "회선번호는 010-XXXX-XXXX 형식이어야 합니다"
    )
    private String lineNumber;

    /**
     * 조회월 (선택)
     * YYYY-MM 형식, 미입력시 당월 조회
     */
    @Pattern(
        regexp = "^\\d{4}-\\d{2}$", 
        message = "조회월은 YYYY-MM 형식이어야 합니다"
    )
    private String inquiryMonth;
}