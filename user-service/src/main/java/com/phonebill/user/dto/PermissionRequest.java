package com.phonebill.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 권한 확인 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequest {
    private String userId;
    private String resource;
    private String action;
}
