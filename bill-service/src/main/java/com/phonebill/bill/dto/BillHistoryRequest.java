package com.phonebill.bill.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 요금조회 이력 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BillHistoryRequest {
    private String userId;
    private String startDate;
    private String endDate;
    private int page;
    private int size;
}
