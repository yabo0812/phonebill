package com.unicorn.phonebill.product.dto.kos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KOS 상품 정보 DTO
 * kos-mock 서비스의 KosProductInfo와 동일한 구조
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KosProductInfo {
    
    @JsonProperty("product_code")
    private String productCode;
    
    @JsonProperty("product_name")
    private String productName;
    
    @JsonProperty("product_type")
    private String productType;
    
    @JsonProperty("monthly_fee")
    private Integer monthlyFee;
    
    @JsonProperty("data_allowance")
    private Integer dataAllowance;
    
    @JsonProperty("voice_allowance")
    private Integer voiceAllowance;
    
    @JsonProperty("sms_allowance")
    private Integer smsAllowance;
    
    @JsonProperty("network_type")
    private String networkType;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("description")
    private String description;
}