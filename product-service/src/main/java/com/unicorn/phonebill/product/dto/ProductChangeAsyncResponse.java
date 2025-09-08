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
 * 상품변경 비동기 처리 응답 DTO (접수 완료 시)
 * API: POST /products/change (202 응답)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductChangeAsyncResponse {

    @NotNull(message = "성공 여부는 필수입니다")
    private Boolean success;

    @Valid
    private AsyncData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AsyncData {

        @NotNull(message = "요청 ID는 필수입니다")
        private String requestId;

        @NotNull(message = "처리 상태는 필수입니다")
        private ProcessStatus processStatus; // PENDING, PROCESSING

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime estimatedCompletionTime;

        private String message;
    }

    public enum ProcessStatus {
        PENDING, PROCESSING
    }

    /**
     * 비동기 접수 응답 생성
     */
    public static ProductChangeAsyncResponse accepted(String requestId, String message) {
        AsyncData data = AsyncData.builder()
                .requestId(requestId)
                .processStatus(ProcessStatus.PROCESSING)
                .estimatedCompletionTime(LocalDateTime.now().plusMinutes(5))
                .message(message != null ? message : "상품 변경이 진행되었습니다")
                .build();

        return ProductChangeAsyncResponse.builder()
                .success(true)
                .data(data)
                .build();
    }
}