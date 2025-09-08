package com.phonebill.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 사용자 권한 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionsResponse {
    
    private String userId;
    private List<Permission> permissions;
    
    /**
     * 권한 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Permission {
        private String permission;
        private String description;
        private Boolean granted;
    }
}