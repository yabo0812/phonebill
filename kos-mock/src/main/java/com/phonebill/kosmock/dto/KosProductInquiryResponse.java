package com.phonebill.kosmock.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * KOS 가입상품 조회 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KOS 가입상품 조회 응답")
public class KosProductInquiryResponse {
    
    @Schema(description = "요청 ID", example = "REQ_20250108_001")
    @JsonProperty("requestId")
    private String requestId;
    
    @Schema(description = "처리 상태", example = "SUCCESS")
    @JsonProperty("procStatus")
    private String procStatus;
    
    @Schema(description = "결과 코드", example = "0000")
    @JsonProperty("resultCode")
    private String resultCode;
    
    @Schema(description = "결과 메시지", example = "정상 처리되었습니다")
    @JsonProperty("resultMessage")
    private String resultMessage;
    
    @Schema(description = "상품 정보")
    @JsonProperty("productInfo")
    private ProductInfo productInfo;
    
    @Schema(description = "고객 정보")
    @JsonProperty("customerInfo")
    private CustomerInfo customerInfo;
    
    /**
     * 상품 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "상품 정보")
    public static class ProductInfo {
        
        @Schema(description = "회선번호", example = "01012345679")
        @JsonProperty("lineNumber")
        private String lineNumber;
        
        @Schema(description = "현재 상품 코드", example = "KT_5G_BASIC")
        @JsonProperty("currentProductCode")
        private String currentProductCode;
        
        @Schema(description = "현재 상품명", example = "KT 5G 베이직")
        @JsonProperty("currentProductName")
        private String currentProductName;
        
        @Schema(description = "월 요금", example = "45000")
        @JsonProperty("monthlyFee")
        private BigDecimal monthlyFee;
        
        @Schema(description = "데이터 허용량", example = "무제한")
        @JsonProperty("dataAllowance")
        private String dataAllowance;
        
        @Schema(description = "음성 허용량", example = "무제한")
        @JsonProperty("voiceAllowance")
        private String voiceAllowance;
        
        @Schema(description = "SMS 허용량", example = "무제한")
        @JsonProperty("smsAllowance")
        private String smsAllowance;
        
        @Schema(description = "상품 상태", example = "ACTIVE")
        @JsonProperty("productStatus")
        private String productStatus;
        
        @Schema(description = "계약일")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("contractDate")
        private LocalDateTime contractDate;
    }
    
    /**
     * 고객 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "고객 정보")
    public static class CustomerInfo {
        
        @Schema(description = "고객명", example = "홍길동")
        @JsonProperty("customerName")
        private String customerName;
        
        @Schema(description = "고객 ID", example = "CUST_001")
        @JsonProperty("customerId")
        private String customerId;
        
        @Schema(description = "통신사 코드", example = "KT")
        @JsonProperty("operatorCode")
        private String operatorCode;
        
        @Schema(description = "회선 상태", example = "ACTIVE")
        @JsonProperty("lineStatus")
        private String lineStatus;
    }
}