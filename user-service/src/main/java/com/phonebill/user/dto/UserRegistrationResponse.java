package com.phonebill.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 등록 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 등록 응답")
public class UserRegistrationResponse {
    
    @Schema(description = "응답 성공 여부", example = "true")
    private boolean success;
    
    @Schema(description = "응답 메시지", example = "사용자가 성공적으로 등록되었습니다")
    private String message;
    
    @Schema(description = "등록된 사용자 정보")
    private UserData data;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "등록된 사용자 데이터")
    public static class UserData {
        
        @Schema(description = "사용자 ID", example = "mvno001")
        private String userId;
        
        @Schema(description = "고객 ID", example = "CU202401001")
        private String customerId;
        
        @Schema(description = "회선번호", example = "010-1234-5678")
        private String lineNumber;
        
        @Schema(description = "사용자 이름", example = "홍길동")
        private String userName;
        
        @Schema(description = "계정 상태", example = "ACTIVE")
        private String accountStatus;
        
        @Schema(description = "등록 시간", example = "2024-01-15T10:30:00")
        private LocalDateTime createdAt;
        
        @Schema(description = "부여된 권한 목록", example = "[\"BILL_INQUIRY\", \"PRODUCT_CHANGE\"]")
        private List<String> permissions;
    }
}