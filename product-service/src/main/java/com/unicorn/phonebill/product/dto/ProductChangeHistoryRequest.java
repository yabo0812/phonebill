package com.unicorn.phonebill.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품변경 이력 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductChangeHistoryRequest {
    private String userId;
    private String startDate;
    private String endDate;
    private String status;
    private int page;
    private int size;
}
