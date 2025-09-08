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
 * 상품변경 처리 응답 DTO (동기 처리 완료 시)
 * API: POST /products/change (200 응답)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductChangeResponse {

    @NotNull(message = "성공 여부는 필수입니다")
    private Boolean success;

    @Valid
    private ProductChangeData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductChangeData {

        @NotNull(message = "요청 ID는 필수입니다")
        private String requestId;

        @NotNull(message = "처리 상태는 필수입니다")
        private ProcessStatus processStatus; // COMPLETED, FAILED

        @NotNull(message = "결과 코드는 필수입니다")
        private String resultCode;

        private String resultMessage;

        @Valid
        private ProductInfoDto changedProduct;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime processedAt;
    }

    public enum ProcessStatus {
        COMPLETED, FAILED
    }

    /**
     * 성공 응답 생성
     */
    public static ProductChangeResponse success(String requestId, String resultCode, 
                                               String resultMessage, ProductInfoDto changedProduct) {
        ProductChangeData data = ProductChangeData.builder()
                .requestId(requestId)
                .processStatus(ProcessStatus.COMPLETED)
                .resultCode(resultCode)
                .resultMessage(resultMessage)
                .changedProduct(changedProduct)
                .processedAt(LocalDateTime.now())
                .build();

        return ProductChangeResponse.builder()
                .success(true)
                .data(data)
                .build();
    }
}