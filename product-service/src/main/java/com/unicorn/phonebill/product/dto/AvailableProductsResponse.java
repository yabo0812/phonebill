package com.unicorn.phonebill.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 변경 가능한 상품 목록 조회 응답 DTO
 * API: GET /products/available
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailableProductsResponse {

    @NotNull(message = "성공 여부는 필수입니다")
    private Boolean success;

    @Valid
    private ProductsData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductsData {

        @NotNull(message = "상품 목록은 필수입니다")
        private List<ProductInfoDto> products;

        private Integer totalCount;
    }

    /**
     * 성공 응답 생성
     */
    public static AvailableProductsResponse success(List<ProductInfoDto> products) {
        ProductsData data = ProductsData.builder()
                .products(products)
                .totalCount(products != null ? products.size() : 0)
                .build();

        return AvailableProductsResponse.builder()
                .success(true)
                .data(data)
                .build();
    }
}