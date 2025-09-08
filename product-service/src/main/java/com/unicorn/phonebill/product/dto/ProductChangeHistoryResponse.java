package com.unicorn.phonebill.product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품변경 이력 조회 응답 DTO
 * API: GET /products/history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductChangeHistoryResponse {

    @NotNull(message = "성공 여부는 필수입니다")
    private Boolean success;

    @Valid
    private HistoryData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HistoryData {

        @NotNull(message = "이력 목록은 필수입니다")
        private List<ProductChangeHistoryItem> history;

        @Valid
        private PaginationInfo pagination;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductChangeHistoryItem {

        @NotNull(message = "요청 ID는 필수입니다")
        private String requestId;

        @NotNull(message = "회선번호는 필수입니다")
        private String lineNumber;

        @NotNull(message = "처리 상태는 필수입니다")
        private String processStatus; // PENDING, PROCESSING, COMPLETED, FAILED

        private String currentProductCode;

        private String currentProductName;

        private String targetProductCode;

        private String targetProductName;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime requestedAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime processedAt;

        private String resultMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationInfo {

        @NotNull(message = "현재 페이지는 필수입니다")
        private Integer page;

        @NotNull(message = "페이지 크기는 필수입니다")
        private Integer size;

        @NotNull(message = "전체 요소 수는 필수입니다")
        private Long totalElements;

        @NotNull(message = "전체 페이지 수는 필수입니다")
        private Integer totalPages;

        private Boolean hasNext;

        private Boolean hasPrevious;
    }

    /**
     * 성공 응답 생성
     */
    public static ProductChangeHistoryResponse success(List<ProductChangeHistoryItem> history, PaginationInfo pagination) {
        HistoryData data = HistoryData.builder()
                .history(history)
                .pagination(pagination)
                .build();

        return ProductChangeHistoryResponse.builder()
                .success(true)
                .data(data)
                .build();
    }
}