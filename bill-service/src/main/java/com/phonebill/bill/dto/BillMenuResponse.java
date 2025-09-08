package com.phonebill.bill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 요금조회 메뉴 응답 DTO
 * 
 * 요금조회 메뉴 화면에 필요한 정보를 담는 응답 객체
 * - 고객 정보 (고객ID, 회선번호)
 * - 조회 가능한 월 목록
 * - 기본 선택된 현재 월
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillMenuResponse {

    /**
     * 고객 정보
     */
    private CustomerInfo customerInfo;

    /**
     * 조회 가능한 월 목록 (YYYY-MM 형식)
     */
    private List<String> availableMonths;

    /**
     * 기본 선택된 현재 월 (YYYY-MM 형식)
     */
    private String currentMonth;

    /**
     * 고객 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        
        /**
         * 고객 ID
         */
        private String customerId;

        /**
         * 회선번호 (010-XXXX-XXXX 형식)
         */
        private String lineNumber;
    }
}