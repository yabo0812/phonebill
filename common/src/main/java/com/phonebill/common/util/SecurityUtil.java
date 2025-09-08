package com.phonebill.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * 보안 유틸리티
 * Spring Security 관련 공통 기능을 제공합니다.
 */
public class SecurityUtil {
    
    /**
     * 현재 인증된 사용자 ID를 반환
     */
    public static Optional<String> getCurrentUserId() {
        return getCurrentUserDetails()
                .map(UserDetails::getUsername);
    }
    
    /**
     * 현재 인증된 사용자 정보를 반환
     */
    public static Optional<UserDetails> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return Optional.of((UserDetails) principal);
        }
        
        return Optional.empty();
    }
    
    /**
     * 현재 인증된 사용자의 권한을 확인
     */
    public static boolean hasAuthority(String authority) {
        return getCurrentUserDetails()
                .map(user -> user.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority)))
                .orElse(false);
    }
    
    /**
     * 현재 인증된 사용자가 특정 역할을 가지고 있는지 확인
     */
    public static boolean hasRole(String role) {
        return hasAuthority("ROLE_" + role);
    }
    
    /**
     * 현재 인증된 사용자가 인증되었는지 확인
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
    
    /**
     * 현재 인증된 사용자의 인증 정보를 반환
     */
    public static Optional<Authentication> getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Optional.ofNullable(authentication);
    }
}
