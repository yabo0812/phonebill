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

/**
 * 상품변경 결과 조회 응답 DTO
 * API: GET /products/change/{requestId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductChangeResultResponse {

    @NotNull(message = "성공 여부는 필수입니다")
    private Boolean success;

    @Valid
    private ProductChangeResult data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductChangeResult {

        @NotNull(message = "요청 ID는 필수입니다")
        private String requestId;

        private String lineNumber;

        @NotNull(message = "처리 상태는 필수입니다")
        private ProcessStatus processStatus; // PENDING, PROCESSING, COMPLETED, FAILED

        private String currentProductCode;

        private String targetProductCode;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime requestedAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime processedAt;

        private String resultCode;

        private String resultMessage;

        private String failureReason;
    }

    public enum ProcessStatus {
        PENDING("접수 대기"),
        PROCESSING("처리 중"),
        COMPLETED("처리 완료"),
        FAILED("처리 실패");

        private final String description;

        ProcessStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 성공 응답 생성
     */
    public static ProductChangeResultResponse success(ProductChangeResult data) {
        return ProductChangeResultResponse.builder()
                .success(true)
                .data(data)
                .build();
    }
}