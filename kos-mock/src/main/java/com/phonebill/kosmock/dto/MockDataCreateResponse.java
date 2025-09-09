package com.phonebill.kosmock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Mock 데이터 생성 응답 DTO
 */
@Data
@Builder
public class MockDataCreateResponse {
    
    @JsonProperty("customer_id")
    private String customerId;
    
    @JsonProperty("line_number")
    private String lineNumber;
    
    @JsonProperty("current_product_code")
    private String currentProductCode;
    
    @JsonProperty("current_product_name")
    private String currentProductName;
    
    @JsonProperty("bill_count_created")
    private int billCountCreated;
    
    @JsonProperty("message")
    private String message;
}