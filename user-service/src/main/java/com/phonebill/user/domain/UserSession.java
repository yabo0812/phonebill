package com.phonebill.user.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 사용자 세션 도메인 모델
 */
@Getter
@Builder
@ToString(exclude = {"sessionToken", "refreshToken"}) // 보안 정보 제외
public class UserSession {
    
    private final String sessionId;
    private final String userId;
    private final String sessionToken;
    private final String refreshToken;
    private final String clientIp;
    private final String userAgent;
    private final boolean autoLoginEnabled;
    private final LocalDateTime expiresAt;
    private final LocalDateTime lastAccessedAt;
    private final LocalDateTime createdAt;
    
    /**
     * 세션 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * 세션 활성 상태 확인
     */
    public boolean isActive() {
        return !isExpired();
    }
    
    /**
     * 세션 만료까지 남은 시간 계산 (초)
     */
    public long getSecondsUntilExpiry() {
        if (isExpired()) {
            return 0;
        }
        
        return java.time.temporal.ChronoUnit.SECONDS.between(LocalDateTime.now(), expiresAt);
    }
    
    /**
     * 마지막 접근으로부터 경과 시간 계산 (초)
     */
    public long getSecondsSinceLastAccess() {
        if (lastAccessedAt == null) {
            return java.time.temporal.ChronoUnit.SECONDS.between(createdAt, LocalDateTime.now());
        }
        
        return java.time.temporal.ChronoUnit.SECONDS.between(lastAccessedAt, LocalDateTime.now());
    }
    
    /**
     * 세션 유휴 상태 확인
     * @param idleTimeoutSeconds 유휴 시간 임계값 (초)
     */
    public boolean isIdle(long idleTimeoutSeconds) {
        return getSecondsSinceLastAccess() > idleTimeoutSeconds;
    }
    
    /**
     * 클라이언트 정보 일치 여부 확인 (보안 검증용)
     */
    public boolean matchesClientInfo(String clientIp, String userAgent) {
        return this.clientIp.equals(clientIp) && this.userAgent.equals(userAgent);
    }
    
    /**
     * 세션 정보 요약 조회용 (보안 정보 제외)
     */
    public SessionSummary toSummary() {
        return SessionSummary.builder()
                .sessionId(sessionId)
                .userId(userId)
                .clientIp(clientIp)
                .autoLoginEnabled(autoLoginEnabled)
                .expiresAt(expiresAt)
                .lastAccessedAt(lastAccessedAt)
                .createdAt(createdAt)
                .active(isActive())
                .build();
    }
    
    /**
     * 세션 요약 정보 (보안 정보 제외)
     */
    @Getter
    @Builder
    @ToString
    public static class SessionSummary {
        private final String sessionId;
        private final String userId;
        private final String clientIp;
        private final boolean autoLoginEnabled;
        private final LocalDateTime expiresAt;
        private final LocalDateTime lastAccessedAt;
        private final LocalDateTime createdAt;
        private final boolean active;
    }
}