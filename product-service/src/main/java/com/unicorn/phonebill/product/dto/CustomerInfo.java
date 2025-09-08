package com.unicorn.phonebill.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerInfo {
    private String customerId;
    private String customerName;
    private String phoneNumber;
    private String email;
    private String address;
    private String customerType;
    private String status;
    private String joinDate;
}
