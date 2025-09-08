package com.phonebill.kosmock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * KOS 상품 변경 응답 DTO
 */
@Data
@Builder
@Schema(description = "KOS 상품 변경 응답")
public class KosProductChangeResponse {
    
    @Schema(description = "요청 ID", example = "REQ_20250108_002")
    private String requestId;
    
    @Schema(description = "처리 결과 코드", example = "0000")
    private String resultCode;
    
    @Schema(description = "처리 결과 메시지", example = "정상 처리되었습니다")
    private String resultMessage;
    
    @Schema(description = "변경 처리 정보")
    private ChangeInfo changeInfo;
    
    @Data
    @Builder
    @Schema(description = "변경 처리 정보")
    public static class ChangeInfo {
        
        @Schema(description = "회선번호", example = "01012345678")
        private String lineNumber;
        
        @Schema(description = "이전 상품 코드", example = "LTE-BASIC-001")
        private String previousProductCode;
        
        @Schema(description = "이전 상품명", example = "LTE 베이직 플랜")
        private String previousProductName;
        
        @Schema(description = "새로운 상품 코드", example = "5G-PREMIUM-001")
        private String newProductCode;
        
        @Schema(description = "새로운 상품명", example = "5G 프리미엄 플랜")
        private String newProductName;
        
        @Schema(description = "변경 적용 일자", example = "20250115")
        private String effectiveDate;
        
        @Schema(description = "변경 처리 상태", example = "SUCCESS")
        private String changeStatus;
        
        @Schema(description = "KOS 주문 번호", example = "KOS20250108001")
        private String kosOrderNumber;
        
        @Schema(description = "예상 처리 완료 시간", example = "2025-01-08T15:30:00")
        private String estimatedCompletionTime;
    }
}