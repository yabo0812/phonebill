package com.phonebill.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 권한 확인 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCheckRequest {
    
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    @NotBlank(message = "권한 코드는 필수입니다")
    private String permissionCode;
    
    @NotBlank(message = "서비스 타입은 필수입니다")
    @Pattern(regexp = "^(BILL_INQUIRY|PRODUCT_CHANGE)$", 
             message = "서비스 타입은 BILL_INQUIRY 또는 PRODUCT_CHANGE만 허용됩니다")
    private String serviceType;
}