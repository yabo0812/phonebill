package com.phonebill.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 권한 확인 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCheckResponse {
    
    private String userId;
    private String permissionCode;
    private String serviceType;
    private Boolean hasPermission;
    private String message;
    private PermissionDetails permissionDetails;
    
    /**
     * 권한 상세 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionDetails {
        private String permission;
        private String description;
        private Boolean granted;
    }
}