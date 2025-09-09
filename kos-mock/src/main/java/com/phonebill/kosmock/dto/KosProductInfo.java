package com.phonebill.kosmock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KOS 상품 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KOS 상품 정보")
public class KosProductInfo {
    
    @JsonProperty("product_code")
    @Schema(description = "상품 코드", example = "5G_BASIC_001")
    private String productCode;
    
    @JsonProperty("product_name")
    @Schema(description = "상품명", example = "5G 베이직 요금제")
    private String productName;
    
    @JsonProperty("product_type")
    @Schema(description = "상품 유형", example = "DATA")
    private String productType;
    
    @JsonProperty("monthly_fee")
    @Schema(description = "월정액", example = "55000")
    private Integer monthlyFee;
    
    @JsonProperty("data_allowance")
    @Schema(description = "데이터 제공량(GB)", example = "100")
    private Integer dataAllowance;
    
    @JsonProperty("voice_allowance")
    @Schema(description = "음성통화 제공량(분)", example = "300")
    private Integer voiceAllowance;
    
    @JsonProperty("sms_allowance")
    @Schema(description = "SMS 제공량(건)", example = "200")
    private Integer smsAllowance;
    
    @JsonProperty("network_type")
    @Schema(description = "네트워크 유형", example = "5G")
    private String networkType;
    
    @JsonProperty("status")
    @Schema(description = "상품 상태", example = "ACTIVE")
    private String status;
    
    @JsonProperty("description")
    @Schema(description = "상품 설명", example = "5G 네트워크를 이용한 대용량 데이터 요금제")
    private String description;
    
}