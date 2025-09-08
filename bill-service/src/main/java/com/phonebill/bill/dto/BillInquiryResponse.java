package com.phonebill.bill.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 요금조회 응답 DTO
 * 
 * 요금조회 결과를 담는 응답 객체
 * - 요청 ID: 조회 요청 추적용
 * - 처리 상태: COMPLETED, PROCESSING, FAILED
 * - 요금 정보: KOS에서 조회된 실제 요금 데이터
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
public class BillInquiryResponse {

    /**
     * 요금조회 요청 ID
     */
    private String requestId;

    /**
     * 처리 상태
     * - COMPLETED: 조회 완료
     * - PROCESSING: 처리 중
     * - FAILED: 조회 실패
     */
    private ProcessStatus status;

    /**
     * 요금 정보 (COMPLETED 상태일 때만 포함)
     */
    private BillInfo billInfo;

    /**
     * 처리 상태 열거형
     */
    public enum ProcessStatus {
        COMPLETED, PROCESSING, FAILED
    }

    /**
     * 요금 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BillInfo {
        
        /**
         * 현재 이용 중인 요금제
         */
        private String productName;

        /**
         * 계약 약정 조건
         */
        private String contractInfo;

        /**
         * 요금 청구 월 (YYYY-MM 형식)
         */
        private String billingMonth;

        /**
         * 청구 요금 금액 (원)
         */
        private Integer totalAmount;

        /**
         * 적용된 할인 내역
         */
        private List<DiscountInfo> discountInfo;

        /**
         * 사용량 정보
         */
        private UsageInfo usage;

        /**
         * 중도 해지 시 비용 (원)
         */
        private Integer terminationFee;

        /**
         * 단말기 할부 잔액 (원)
         */
        private Integer deviceInstallment;

        /**
         * 납부 정보
         */
        private PaymentInfo paymentInfo;
    }

    /**
     * 할인 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountInfo {
        
        /**
         * 할인 명칭
         */
        private String name;

        /**
         * 할인 금액 (원)
         */
        private Integer amount;
    }

    /**
     * 사용량 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageInfo {
        
        /**
         * 통화 사용량
         */
        private String voice;

        /**
         * SMS 사용량
         */
        private String sms;

        /**
         * 데이터 사용량
         */
        private String data;
    }

    /**
     * 납부 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        
        /**
         * 요금 청구일 (YYYY-MM-DD 형식)
         */
        private String billingDate;

        /**
         * 납부 상태 (PAID, UNPAID, OVERDUE)
         */
        private PaymentStatus paymentStatus;

        /**
         * 납부 방법
         */
        private String paymentMethod;
    }

    /**
     * 납부 상태 열거형
     */
    public enum PaymentStatus {
        PAID, UNPAID, OVERDUE
    }
}