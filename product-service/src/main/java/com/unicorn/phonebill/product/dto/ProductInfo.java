package com.unicorn.phonebill.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 상품 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInfo {
    private String productId;
    private String productName;
    private String productType;
    private String description;
    private BigDecimal price;
    private String status;
    private String category;
    private String validFrom;
    private String validTo;
}
