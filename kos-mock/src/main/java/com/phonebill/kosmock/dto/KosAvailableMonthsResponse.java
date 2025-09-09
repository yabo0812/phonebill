package com.phonebill.kosmock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * KOS 데이터 보유 월 목록 응답 DTO
 * 
 * 특정 회선번호의 실제 요금 데이터가 있는 월 목록을 반환
 * 
 * @author 이개발(백엔더)
 * @version 1.0.1
 * @since 2025-09-09
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KosAvailableMonthsResponse {

    /**
     * 처리 결과 코드
     * - 0000: 성공
     * - 1001: 존재하지 않는 회선번호
     * - 1002: 비활성 상태의 회선
     */
    @JsonProperty("resultCode")
    private String resultCode;

    /**
     * 처리 결과 메시지
     */
    @JsonProperty("resultMessage")
    private String resultMessage;

    /**
     * 회선번호
     */
    @JsonProperty("lineNumber")
    private String lineNumber;

    /**
     * 데이터가 있는 월 목록 (yyyy-MM 형식)
     * 예: ["2025-09", "2025-08", "2025-07"]
     */
    @JsonProperty("availableMonths")
    private List<String> availableMonths;
}