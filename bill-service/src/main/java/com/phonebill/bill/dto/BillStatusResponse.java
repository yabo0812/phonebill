package com.phonebill.bill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 요금조회 상태 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillStatusResponse {
    private String requestId;
    private String status;
    private String message;
    private String processedAt;
}
