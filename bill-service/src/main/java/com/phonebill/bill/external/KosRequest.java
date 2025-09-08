package com.phonebill.bill.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * KOS 시스템 요청 모델
 * 
 * 통신사 백엔드 시스템(KOS)으로 전송하는 요청 데이터 구조
 * - 요금조회 요청에 필요한 정보 포함
 * - KOS API 스펙에 맞춘 필드명 매핑
 * - 요청 추적을 위한 메타 정보 포함
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KosRequest {

    /**
     * 회선번호 (KOS 필드명: lineNum)
     */
    @JsonProperty("lineNum")
    private String lineNumber;

    /**
     * 조회월 (KOS 필드명: searchMonth, YYYY-MM 형식)
     */
    @JsonProperty("searchMonth")
    private String inquiryMonth;

    /**
     * 서비스 구분 코드 (KOS 필드명: svcDiv)
     * - BILL_INQ: 요금조회
     * - BILL_DETAIL: 상세조회
     */
    @JsonProperty("svcDiv")
    @Builder.Default
    private String serviceCode = "BILL_INQ";

    /**
     * 요청 시스템 코드 (KOS 필드명: reqSysCode)
     */
    @JsonProperty("reqSysCode")
    @Builder.Default
    private String requestSystemCode = "MVNO";

    /**
     * 요청 채널 코드 (KOS 필드명: reqChnlCode)
     * - WEB: 웹
     * - APP: 모바일앱
     * - API: API
     */
    @JsonProperty("reqChnlCode")
    @Builder.Default
    private String requestChannelCode = "API";

    /**
     * 요청자 ID (KOS 필드명: reqUserId)
     */
    @JsonProperty("reqUserId")
    private String requestUserId;

    /**
     * 요청일시 (KOS 필드명: reqDttm, YYYY-MM-DD HH:MM:SS 형식)
     */
    @JsonProperty("reqDttm")
    private LocalDateTime requestTime;

    /**
     * 요청 고유번호 (KOS 필드명: reqSeqNo)
     */
    @JsonProperty("reqSeqNo")
    private String requestSequenceNumber;

    /**
     * 고객 구분 코드 (KOS 필드명: custDiv)
     * - PERS: 개인
     * - CORP: 법인
     */
    @JsonProperty("custDiv")
    @Builder.Default
    private String customerType = "PERS";

    /**
     * 인증 토큰 (KOS 필드명: authToken)
     */
    @JsonProperty("authToken")
    private String authToken;

    /**
     * 응답 형식 (KOS 필드명: respFormat)
     */
    @JsonProperty("respFormat")
    @Builder.Default
    private String responseFormat = "JSON";

    /**
     * 타임아웃 설정 (초, KOS 필드명: timeout)
     */
    @JsonProperty("timeout")
    @Builder.Default
    private Integer timeout = 30;

    // === Static Factory Methods ===

    /**
     * 요금조회 요청 생성
     * 
     * @param lineNumber 회선번호
     * @param inquiryMonth 조회월
     * @param requestUserId 요청자 ID
     * @return KOS 요청 객체
     */
    public static KosRequest createBillInquiryRequest(String lineNumber, String inquiryMonth, String requestUserId) {
        return KosRequest.builder()
                .lineNumber(lineNumber)
                .inquiryMonth(inquiryMonth)
                .requestUserId(requestUserId)
                .requestTime(LocalDateTime.now())
                .requestSequenceNumber(generateSequenceNumber())
                .serviceCode("BILL_INQ")
                .build();
    }

    /**
     * 상세조회 요청 생성
     * 
     * @param lineNumber 회선번호
     * @param inquiryMonth 조회월
     * @param requestUserId 요청자 ID
     * @return KOS 요청 객체
     */
    public static KosRequest createBillDetailRequest(String lineNumber, String inquiryMonth, String requestUserId) {
        return KosRequest.builder()
                .lineNumber(lineNumber)
                .inquiryMonth(inquiryMonth)
                .requestUserId(requestUserId)
                .requestTime(LocalDateTime.now())
                .requestSequenceNumber(generateSequenceNumber())
                .serviceCode("BILL_DETAIL")
                .build();
    }

    // === Helper Methods ===

    /**
     * 요청 순번 생성
     * 
     * @return 요청 순번
     */
    private static String generateSequenceNumber() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * 인증 토큰 설정
     * 
     * @param authToken 인증 토큰
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    /**
     * 요청자 ID 설정
     * 
     * @param requestUserId 요청자 ID
     */
    public void setRequestUserId(String requestUserId) {
        this.requestUserId = requestUserId;
    }

    /**
     * 요청 유효성 검증
     * 
     * @return 유효한 요청인지 여부
     */
    public boolean isValid() {
        return lineNumber != null && !lineNumber.trim().isEmpty() &&
               inquiryMonth != null && !inquiryMonth.trim().isEmpty() &&
               requestUserId != null && !requestUserId.trim().isEmpty();
    }

    /**
     * 요청 정보 요약
     * 
     * @return 요청 요약 정보
     */
    public String getSummary() {
        return String.format("KOS 요청 - 회선: %s, 조회월: %s, 서비스: %s", 
                lineNumber, inquiryMonth, serviceCode);
    }
}