package com.phonebill.bill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 할인 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountInfo {
    private String discountName;
    private String discountType;
    private BigDecimal discountAmount;
    private String description;
    private String validFrom;
    private String validTo;
}
