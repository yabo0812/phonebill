package com.phonebill.common.security;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 인증된 사용자 정보
 * JWT 토큰에서 추출된 사용자 정보를 담는 Principal 객체
 */
@Getter
@Builder
@RequiredArgsConstructor
public class UserPrincipal {
    
    /**
     * 사용자 고유 ID
     */
    private final String userId;
    
    /**
     * 사용자명
     */
    private final String username;
    
    /**
     * 사용자 권한
     */
    private final String authority;
    
    /**
     * 고객 ID
     */
    private final String customerId;
    
    /**
     * 회선번호
     */
    private final String lineNumber;
    
    /**
     * 사용자 ID 반환 (별칭)
     */
    public String getName() {
        return userId;
    }
    
    /**
     * 관리자 권한 여부 확인
     */
    public boolean isAdmin() {
        return "ADMIN".equals(authority);
    }
    
    /**
     * 일반 사용자 권한 여부 확인
     */
    public boolean isUser() {
        return "USER".equals(authority) || authority == null;
    }
}