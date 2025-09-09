package com.phonebill.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 세션 엔티티
 * 사용자의 로그인 세션 정보를 관리
 */
@Entity
@Table(name = "auth_user_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuthUserSessionEntity extends BaseTimeEntity {
    
    @Id
    @Column(name = "session_id", length = 100)
    @Builder.Default
    private String sessionId = java.util.UUID.randomUUID().toString();
    
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;
    
    @Column(name = "session_token", nullable = false, length = 500)
    private String sessionToken;
    
    @Column(name = "refresh_token", length = 500)
    private String refreshToken;
    
    @Column(name = "client_ip", length = 45)
    private String clientIp;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "auto_login_enabled")
    @Builder.Default
    private Boolean autoLoginEnabled = false;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "last_accessed_at")
    @Builder.Default
    private LocalDateTime lastAccessedAt = LocalDateTime.now();
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    /**
     * 세션 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
    
    /**
     * 마지막 접근 시간 업데이트
     */
    public void updateLastAccessedAt() {
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    /**
     * 세션 토큰 갱신
     */
    public void updateSessionToken(String newSessionToken, LocalDateTime newExpiresAt) {
        this.sessionToken = newSessionToken;
        this.expiresAt = newExpiresAt;
        this.updateLastAccessedAt();
    }
    
    /**
     * 리프레시 토큰 갱신
     */
    public void updateRefreshToken(String newRefreshToken) {
        this.refreshToken = newRefreshToken;
        this.updateLastAccessedAt();
    }
    
    /**
     * 세션 활성 상태 확인
     */
    public boolean isActive() {
        return this.isActive && !isExpired();
    }
    
    /**
     * 세션 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
    
    /**
     * 세션 활성화
     */
    public void activate() {
        this.isActive = true;
    }
}