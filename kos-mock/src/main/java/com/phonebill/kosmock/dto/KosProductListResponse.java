package com.phonebill.kosmock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * KOS 상품 목록 조회 응답
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KOS 상품 목록 조회 응답")
public class KosProductListResponse {
    
    @JsonProperty("result_code")
    @Schema(description = "결과 코드", example = "0000")
    private String resultCode;
    
    @JsonProperty("result_message")
    @Schema(description = "결과 메시지", example = "상품 목록 조회 성공")
    private String resultMessage;
    
    @JsonProperty("product_count")
    @Schema(description = "조회된 상품 개수", example = "5")
    private Integer productCount;
    
    @JsonProperty("products")
    @Schema(description = "상품 목록")
    private List<KosProductInfo> products;
    
}