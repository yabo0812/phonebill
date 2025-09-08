package com.unicorn.phonebill.product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 고객 정보 조회 응답 DTO
 * API: GET /products/customer/{lineNumber}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerInfoResponse {

    @NotNull(message = "성공 여부는 필수입니다")
    private Boolean success;

    @Valid
    private CustomerInfo data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomerInfo {

        @NotBlank(message = "고객 ID는 필수입니다")
        private String customerId;

        @NotBlank(message = "회선번호는 필수입니다")
        @Pattern(regexp = "^010[0-9]{8}$", message = "회선번호는 010으로 시작하는 11자리 숫자여야 합니다")
        private String lineNumber;

        @NotBlank(message = "고객명은 필수입니다")
        private String customerName;

        @NotNull(message = "현재 상품 정보는 필수입니다")
        @Valid
        private ProductInfoDto currentProduct;

        @NotBlank(message = "회선 상태는 필수입니다")
        private String lineStatus; // ACTIVE, SUSPENDED, TERMINATED

        @Valid
        private ContractInfo contractInfo;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class ContractInfo {

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            private LocalDate contractDate;

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            private LocalDate termEndDate;

            private BigDecimal earlyTerminationFee;
        }
    }

    /**
     * 성공 응답 생성
     */
    public static CustomerInfoResponse success(CustomerInfo data) {
        return CustomerInfoResponse.builder()
                .success(true)
                .data(data)
                .build();
    }
}