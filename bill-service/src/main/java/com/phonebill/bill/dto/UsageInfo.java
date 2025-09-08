package com.phonebill.bill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 사용량 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageInfo {
    private String serviceType;
    private Long usageAmount;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String description;
}
