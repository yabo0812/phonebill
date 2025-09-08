package com.unicorn.phonebill.product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품변경 메뉴 조회 응답 DTO
 */
@Getter
@Builder
@Schema(description = "상품변경 메뉴 조회 응답")
public class ProductMenuResponse {

    @Schema(description = "응답 성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "메뉴 데이터")
    private final MenuData data;

    @Schema(description = "응답 시간")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;

    @Getter
    @Builder
    @Schema(description = "메뉴 데이터")
    public static class MenuData {
        
        @Schema(description = "고객 ID", example = "CUST001")
        private final String customerId;

        @Schema(description = "회선번호", example = "01012345678")
        private final String lineNumber;

        @Schema(description = "현재 상품 정보")
        private final ProductInfoDto currentProduct;

        @Schema(description = "메뉴 항목 목록")
        private final List<MenuItem> menuItems;
    }

    @Getter
    @Builder
    @Schema(description = "메뉴 항목")
    public static class MenuItem {
        
        @Schema(description = "메뉴 ID", example = "MENU001")
        private final String menuId;

        @Schema(description = "메뉴명", example = "상품변경")
        private final String menuName;

        @Schema(description = "사용 가능 여부", example = "true")
        private final boolean available;

        @Schema(description = "메뉴 설명", example = "현재 이용 중인 상품을 다른 상품으로 변경합니다")
        private final String description;
    }

    public static ProductMenuResponse success(MenuData data) {
        return ProductMenuResponse.builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}