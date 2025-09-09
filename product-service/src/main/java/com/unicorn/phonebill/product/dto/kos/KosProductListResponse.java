package com.unicorn.phonebill.product.dto.kos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * KOS 상품 목록 응답 DTO
 * kos-mock 서비스의 KosProductListResponse와 동일한 구조
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KosProductListResponse {
    
    @JsonProperty("total_count")
    private Integer totalCount;
    
    @JsonProperty("products")
    private List<KosProductInfo> products;
}