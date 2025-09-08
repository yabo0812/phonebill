package com.unicorn.phonebill.gateway.dto;

import java.time.Instant;

/**
 * JWT 토큰 검증 결과 DTO
 * 
 * JWT 토큰 검증 결과와 관련 정보를 담는 데이터 전송 객체입니다.
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-01-08
 */
public class TokenValidationResult {
    
    private final boolean valid;
    private final String userId;
    private final String userRole;
    private final Instant expiresAt;
    private final boolean needsRefresh;
    private final String failureReason;

    private TokenValidationResult(boolean valid, String userId, String userRole, 
                                 Instant expiresAt, boolean needsRefresh, String failureReason) {
        this.valid = valid;
        this.userId = userId;
        this.userRole = userRole;
        this.expiresAt = expiresAt;
        this.needsRefresh = needsRefresh;
        this.failureReason = failureReason;
    }

    /**
     * 유효한 토큰 결과 생성
     * 
     * @param userId 사용자 ID
     * @param userRole 사용자 역할
     * @param expiresAt 만료 시간
     * @param needsRefresh 갱신 필요 여부
     * @return TokenValidationResult
     */
    public static TokenValidationResult valid(String userId, String userRole, 
                                            Instant expiresAt, boolean needsRefresh) {
        return new TokenValidationResult(true, userId, userRole, expiresAt, needsRefresh, null);
    }

    /**
     * 유효한 토큰 결과 생성 (갱신 불필요)
     * 
     * @param userId 사용자 ID
     * @param userRole 사용자 역할
     * @param expiresAt 만료 시간
     * @return TokenValidationResult
     */
    public static TokenValidationResult valid(String userId, String userRole, Instant expiresAt) {
        return new TokenValidationResult(true, userId, userRole, expiresAt, false, null);
    }

    /**
     * 유효하지 않은 토큰 결과 생성
     * 
     * @param failureReason 실패 원인
     * @return TokenValidationResult
     */
    public static TokenValidationResult invalid(String failureReason) {
        return new TokenValidationResult(false, null, null, null, false, failureReason);
    }

    /**
     * 토큰 유효성 여부
     * 
     * @return 유효성 여부
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 사용자 ID
     * 
     * @return 사용자 ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 사용자 역할
     * 
     * @return 사용자 역할
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * 토큰 만료 시간
     * 
     * @return 만료 시간
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * 토큰 갱신 필요 여부
     * 
     * @return 갱신 필요 여부
     */
    public boolean needsRefresh() {
        return needsRefresh;
    }

    /**
     * 검증 실패 원인
     * 
     * @return 실패 원인
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * 토큰이 유효하지 않은지 확인
     * 
     * @return 무효성 여부
     */
    public boolean isInvalid() {
        return !valid;
    }

    /**
     * 사용자 정보가 있는지 확인
     * 
     * @return 사용자 정보 존재 여부
     */
    public boolean hasUserInfo() {
        return valid && userId != null && !userId.trim().isEmpty();
    }

    /**
     * 관리자 권한 확인
     * 
     * @return 관리자 권한 여부
     */
    public boolean isAdmin() {
        return valid && "ADMIN".equalsIgnoreCase(userRole);
    }

    /**
     * VIP 사용자 확인
     * 
     * @return VIP 사용자 여부
     */
    public boolean isVipUser() {
        return valid && ("VIP".equalsIgnoreCase(userRole) || "PREMIUM".equalsIgnoreCase(userRole));
    }

    @Override
    public String toString() {
        if (valid) {
            return String.format(
                "TokenValidationResult{valid=true, userId='%s', userRole='%s', expiresAt=%s, needsRefresh=%s}",
                userId, userRole, expiresAt, needsRefresh
            );
        } else {
            return String.format(
                "TokenValidationResult{valid=false, failureReason='%s'}",
                failureReason
            );
        }
    }
}