package com.phonebill.user.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 도메인 모델
 * 비즈니스 로직을 포함하는 사용자 정보
 */
@Getter
@Builder
@ToString(exclude = "permissions") // 순환 참조 방지
public class User {
    
    private final String userId;
    private final String customerId;
    private final String lineNumber;
    private final UserStatus status;
    private final Integer failedLoginCount;
    private final LocalDateTime lastFailedLoginAt;
    private final LocalDateTime accountLockedUntil;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime lastPasswordChangedAt;
    private final List<String> permissions;
    
    /**
     * 사용자 상태 열거형
     */
    public enum UserStatus {
        ACTIVE,    // 활성
        LOCKED,    // 잠금
        SUSPENDED, // 정지
        INACTIVE   // 비활성
    }
    
    /**
     * 계정 활성 상태 확인
     */
    public boolean isAccountActive() {
        return status == UserStatus.ACTIVE && !isAccountLocked();
    }
    
    /**
     * 계정 잠금 상태 확인
     */
    public boolean isAccountLocked() {
        if (status != UserStatus.LOCKED) {
            return false;
        }
        
        // 잠금 해제 시간이 지났으면 잠금 해제로 판단
        return accountLockedUntil == null || LocalDateTime.now().isBefore(accountLockedUntil);
    }
    
    /**
     * 로그인 실패 임계치 확인
     * @param maxFailedAttempts 최대 허용 실패 횟수
     */
    public boolean isLoginFailureThresholdExceeded(int maxFailedAttempts) {
        return failedLoginCount != null && failedLoginCount >= maxFailedAttempts;
    }
    
    /**
     * 특정 권한 보유 여부 확인
     */
    public boolean hasPermission(String permissionCode) {
        return permissions != null && permissions.contains(permissionCode);
    }
    
    /**
     * 서비스별 권한 보유 여부 확인
     * @param serviceType 서비스 타입 (BILL_INQUIRY, PRODUCT_CHANGE)
     */
    public boolean hasServicePermission(String serviceType) {
        return hasPermission(serviceType);
    }
    
    /**
     * 비밀번호 변경이 필요한지 확인
     * @param passwordChangeIntervalDays 비밀번호 변경 주기 (일)
     */
    public boolean isPasswordChangeRequired(int passwordChangeIntervalDays) {
        if (lastPasswordChangedAt == null) {
            return true; // 비밀번호 변경 이력이 없으면 변경 필요
        }
        
        LocalDateTime changeRequiredDate = lastPasswordChangedAt.plusDays(passwordChangeIntervalDays);
        return LocalDateTime.now().isAfter(changeRequiredDate);
    }
    
    /**
     * 마지막 로그인으로부터 경과된 일수 계산
     */
    public long getDaysSinceLastLogin() {
        if (lastLoginAt == null) {
            return Long.MAX_VALUE; // 로그인 이력이 없으면 최대값 반환
        }
        
        return java.time.temporal.ChronoUnit.DAYS.between(lastLoginAt, LocalDateTime.now());
    }
    
    /**
     * 사용자 기본 정보 조회용 빌더 (보안 정보 제외)
     */
    public static User createBasicInfo(String userId, String customerId, String lineNumber, 
                                       UserStatus status, LocalDateTime lastLoginAt, 
                                       List<String> permissions) {
        return User.builder()
                .userId(userId)
                .customerId(customerId)
                .lineNumber(lineNumber)
                .status(status)
                .lastLoginAt(lastLoginAt)
                .permissions(permissions)
                .build();
    }
}