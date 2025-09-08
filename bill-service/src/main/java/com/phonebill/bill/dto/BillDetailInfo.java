package com.phonebill.bill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 요금 상세 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillDetailInfo {
    private String itemName;
    private String itemType;
    private BigDecimal amount;
    private String description;
    private String category;
}
