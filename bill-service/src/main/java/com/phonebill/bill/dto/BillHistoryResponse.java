package com.phonebill.bill.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 요금조회 이력 응답 DTO
 * 
 * 요금조회 이력 목록을 담는 응답 객체
 * - 이력 항목 리스트
 * - 페이징 정보
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillHistoryResponse {

    /**
     * 요금조회 이력 목록
     */
    private List<BillHistoryItem> items;

    /**
     * 페이징 정보
     */
    private PaginationInfo pagination;

    /**
     * 요금조회 이력 항목 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillHistoryItem {
        
        /**
         * 요금조회 요청 ID
         */
        private String requestId;

        /**
         * 회선번호
         */
        private String lineNumber;

        /**
         * 조회월 (YYYY-MM 형식)
         */
        private String inquiryMonth;

        /**
         * 요청일시
         */
        private LocalDateTime requestTime;

        /**
         * 처리일시
         */
        private LocalDateTime processTime;

        /**
         * 처리 결과
         */
        private BillInquiryResponse.ProcessStatus status;

        /**
         * 결과 요약 (성공시 요금제명과 금액)
         */
        private String resultSummary;
    }

    /**
     * 페이징 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        
        /**
         * 현재 페이지
         */
        private Integer currentPage;

        /**
         * 전체 페이지 수
         */
        private Integer totalPages;

        /**
         * 전체 항목 수
         */
        private Long totalItems;

        /**
         * 페이지 크기
         */
        private Integer pageSize;

        /**
         * 다음 페이지 존재 여부
         */
        private Boolean hasNext;

        /**
         * 이전 페이지 존재 여부
         */
        private Boolean hasPrevious;
    }
}