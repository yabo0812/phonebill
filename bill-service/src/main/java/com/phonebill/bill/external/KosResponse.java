package com.phonebill.bill.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * KOS 시스템 응답 모델
 * 
 * 통신사 백엔드 시스템(KOS)에서 수신하는 응답 데이터 구조
 * - 요금조회 결과 데이터 포함
 * - KOS API 스펙에 맞춘 필드명 매핑
 * - 내부 모델로 변환하기 위한 구조 제공
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KosResponse {

    /**
     * 요청 ID (KOS 필드명: reqId)
     */
    @JsonProperty("reqId")
    private String requestId;

    /**
     * 처리 상태 (KOS 필드명: procStatus)
     * - SUCCESS: 성공
     * - PROCESSING: 처리 중
     * - FAILED: 실패
     */
    @JsonProperty("procStatus")
    private String status;

    /**
     * 결과 코드 (KOS 필드명: resultCode)
     * - 0000: 성공
     * - 기타: 오류 코드
     */
    @JsonProperty("resultCode")
    private String resultCode;

    /**
     * 결과 메시지 (KOS 필드명: resultMsg)
     */
    @JsonProperty("resultMsg")
    private String resultMessage;

    /**
     * 응답일시 (KOS 필드명: respDttm)
     */
    @JsonProperty("respDttm")
    private LocalDateTime responseTime;

    /**
     * 처리 시간 (밀리초, KOS 필드명: procTimeMs)
     */
    @JsonProperty("procTimeMs")
    private Long processingTimeMs;

    /**
     * 요금 데이터 (KOS 필드명: billData)
     */
    @JsonProperty("billData")
    private BillData billData;

    /**
     * 추가 정보 (KOS 필드명: addInfo)
     */
    @JsonProperty("addInfo")
    private String additionalInfo;

    /**
     * 요금 데이터 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillData {

        /**
         * 요금제명 (KOS 필드명: prodNm)
         */
        @JsonProperty("prodNm")
        private String productName;

        /**
         * 계약 정보 (KOS 필드명: contractInfo)
         */
        @JsonProperty("contractInfo")
        private String contractInfo;

        /**
         * 청구월 (KOS 필드명: billMonth)
         */
        @JsonProperty("billMonth")
        private String billingMonth;

        /**
         * 청구 금액 (KOS 필드명: billAmt)
         */
        @JsonProperty("billAmt")
        private Integer totalAmount;

        /**
         * 할인 정보 목록 (KOS 필드명: discList)
         */
        @JsonProperty("discList")
        private List<DiscountData> discounts;

        /**
         * 사용량 정보 (KOS 필드명: usageInfo)
         */
        @JsonProperty("usageInfo")
        private UsageData usage;

        /**
         * 위약금 (KOS 필드명: penaltyAmt)
         */
        @JsonProperty("penaltyAmt")
        private Integer terminationFee;

        /**
         * 할부금 잔액 (KOS 필드명: installAmt)
         */
        @JsonProperty("installAmt")
        private Integer deviceInstallment;

        /**
         * 결제 정보 (KOS 필드명: payInfo)
         */
        @JsonProperty("payInfo")
        private PaymentData payment;
    }

    /**
     * 할인 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountData {

        /**
         * 할인명 (KOS 필드명: discNm)
         */
        @JsonProperty("discNm")
        private String name;

        /**
         * 할인 금액 (KOS 필드명: discAmt)
         */
        @JsonProperty("discAmt")
        private Integer amount;

        /**
         * 할인 유형 (KOS 필드명: discType)
         */
        @JsonProperty("discType")
        private String type;

        /**
         * 할인 기간 (KOS 필드명: discPeriod)
         */
        @JsonProperty("discPeriod")
        private String period;
    }

    /**
     * 사용량 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageData {

        /**
         * 통화 사용량 (KOS 필드명: voiceUsage)
         */
        @JsonProperty("voiceUsage")
        private String voice;

        /**
         * SMS 사용량 (KOS 필드명: smsUsage)
         */
        @JsonProperty("smsUsage")
        private String sms;

        /**
         * 데이터 사용량 (KOS 필드명: dataUsage)
         */
        @JsonProperty("dataUsage")
        private String data;

        /**
         * 기본료 (KOS 필드명: basicFee)
         */
        @JsonProperty("basicFee")
        private Integer basicFee;

        /**
         * 초과료 (KOS 필드명: overageFee)
         */
        @JsonProperty("overageFee")
        private Integer overageFee;
    }

    /**
     * 결제 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentData {

        /**
         * 청구일 (KOS 필드명: billDate)
         */
        @JsonProperty("billDate")
        private String billingDate;

        /**
         * 결제 상태 (KOS 필드명: payStatus)
         * - PAID: 결제 완료
         * - UNPAID: 미결제
         * - OVERDUE: 연체
         */
        @JsonProperty("payStatus")
        private String status;

        /**
         * 결제 방법 (KOS 필드명: payMethod)
         */
        @JsonProperty("payMethod")
        private String method;

        /**
         * 결제일 (KOS 필드명: payDate)
         */
        @JsonProperty("payDate")
        private String paymentDate;

        /**
         * 결제 은행 (KOS 필드명: payBank)
         */
        @JsonProperty("payBank")
        private String paymentBank;

        /**
         * 계좌번호 (마스킹, KOS 필드명: acctNum)
         */
        @JsonProperty("acctNum")
        private String accountNumber;
    }

    // === Helper Methods ===

    /**
     * 성공 응답인지 확인
     * 
     * @return 성공 여부
     */
    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status) && "0000".equals(resultCode);
    }

    /**
     * 처리 중 상태인지 확인
     * 
     * @return 처리 중 여부
     */
    public boolean isProcessing() {
        return "PROCESSING".equalsIgnoreCase(status);
    }

    /**
     * 실패 응답인지 확인
     * 
     * @return 실패 여부
     */
    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status) || (!"0000".equals(resultCode) && resultCode != null);
    }

    /**
     * 요금 데이터 존재 여부 확인
     * 
     * @return 요금 데이터 존재 여부
     */
    public boolean hasBillData() {
        return billData != null && billData.getTotalAmount() != null;
    }

    /**
     * 오류 정보 조회
     * 
     * @return 오류 정보 (결과코드: 결과메시지)
     */
    public String getErrorInfo() {
        if (isFailed()) {
            return String.format("%s: %s", resultCode, resultMessage);
        }
        return null;
    }

    /**
     * 응답 요약 정보
     * 
     * @return 응답 요약
     */
    public String getSummary() {
        if (isSuccess() && hasBillData()) {
            return String.format("KOS 응답 성공 - 요금제: %s, 금액: %,d원", 
                    billData.getProductName(), billData.getTotalAmount());
        } else if (isProcessing()) {
            return "KOS 응답 - 처리 중";
        } else {
            return String.format("KOS 응답 실패 - %s", getErrorInfo());
        }
    }

    /**
     * 처리 시간이 느린지 확인 (임계값: 3초)
     * 
     * @return 느린 응답 여부
     */
    public boolean isSlowResponse() {
        return processingTimeMs != null && processingTimeMs > 3000;
    }
}