package com.phonebill.kosmock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * KOS 요금 조회 응답 DTO
 */
@Data
@Builder
@Schema(description = "KOS 요금 조회 응답")
public class KosBillInquiryResponse {
    
    @Schema(description = "요청 ID", example = "REQ_20250108_001")
    private String requestId;
    
    @Schema(description = "처리 상태", example = "SUCCESS")
    private String procStatus;
    
    @Schema(description = "처리 결과 코드", example = "0000")
    private String resultCode;
    
    @Schema(description = "처리 결과 메시지", example = "정상 처리되었습니다")
    private String resultMessage;
    
    @Schema(description = "요금 정보")
    private BillInfo billInfo;
    
    @Schema(description = "고객 정보")
    private CustomerInfo customerInfo;
    
    @Data
    @Builder
    @Schema(description = "요금 정보")
    public static class BillInfo {
        
        @Schema(description = "회선번호", example = "01012345678")
        private String lineNumber;
        
        @Schema(description = "청구월", example = "202501")
        private String billingMonth;
        
        @Schema(description = "상품 코드", example = "5G-PREMIUM-001")
        private String productCode;
        
        @Schema(description = "상품명", example = "5G 프리미엄 플랜")
        private String productName;
        
        @Schema(description = "월 기본료", example = "89000")
        private BigDecimal monthlyFee;
        
        @Schema(description = "사용료", example = "15000")
        private BigDecimal usageFee;
        
        @Schema(description = "할인 금액", example = "5000")
        private BigDecimal discountAmount;
        
        @Schema(description = "총 요금", example = "99000")
        private BigDecimal totalFee;
        
        @Schema(description = "데이터 사용량", example = "150GB")
        private String dataUsage;
        
        @Schema(description = "음성 사용량", example = "250분")
        private String voiceUsage;
        
        @Schema(description = "SMS 사용량", example = "50건")
        private String smsUsage;
        
        @Schema(description = "청구 상태", example = "CONFIRMED")
        private String billStatus;
        
        @Schema(description = "납부 기한", example = "20250125")
        private String dueDate;
    }
    
    @Data
    @Builder
    @Schema(description = "고객 정보")
    public static class CustomerInfo {
        
        @Schema(description = "고객명", example = "김테스트")
        private String customerName;
        
        @Schema(description = "고객 ID", example = "CUST000001")
        private String customerId;
        
        @Schema(description = "통신사업자 코드", example = "KT")
        private String operatorCode;
        
        @Schema(description = "회선 상태", example = "ACTIVE")
        private String lineStatus;
    }
}