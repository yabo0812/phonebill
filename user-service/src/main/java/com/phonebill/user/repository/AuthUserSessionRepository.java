package com.phonebill.user.repository;

import com.phonebill.user.entity.AuthUserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 세션 Repository
 */
@Repository
public interface AuthUserSessionRepository extends JpaRepository<AuthUserSessionEntity, String> {
    
    /**
     * 사용자 ID로 활성 세션 조회
     */
    List<AuthUserSessionEntity> findByUserIdAndExpiresAtAfter(String userId, LocalDateTime currentTime);
    
    /**
     * 세션 토큰으로 세션 조회
     */
    Optional<AuthUserSessionEntity> findBySessionToken(String sessionToken);
    
    /**
     * 리프레시 토큰으로 세션 조회
     */
    Optional<AuthUserSessionEntity> findByRefreshToken(String refreshToken);
    
    /**
     * 특정 사용자의 모든 세션 조회
     */
    List<AuthUserSessionEntity> findByUserId(String userId);
    
    /**
     * 만료된 세션 조회
     */
    List<AuthUserSessionEntity> findByExpiresAtBefore(LocalDateTime expirationTime);
    
    /**
     * 특정 사용자의 활성 세션 수 조회
     */
    @Query("SELECT COUNT(s) FROM AuthUserSessionEntity s WHERE s.userId = :userId AND s.expiresAt > :currentTime")
    long countActiveSessionsByUserId(@Param("userId") String userId, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 마지막 접근 시간 업데이트
     */
    @Modifying
    @Query("UPDATE AuthUserSessionEntity s SET s.lastAccessedAt = :accessTime " +
           "WHERE s.sessionId = :sessionId")
    int updateLastAccessedTime(@Param("sessionId") String sessionId, 
                               @Param("accessTime") LocalDateTime accessTime);
    
    /**
     * 세션 토큰 업데이트
     */
    @Modifying
    @Query("UPDATE AuthUserSessionEntity s SET s.sessionToken = :sessionToken, " +
           "s.expiresAt = :expiresAt, s.lastAccessedAt = :accessTime " +
           "WHERE s.sessionId = :sessionId")
    int updateSessionToken(@Param("sessionId") String sessionId, 
                           @Param("sessionToken") String sessionToken,
                           @Param("expiresAt") LocalDateTime expiresAt,
                           @Param("accessTime") LocalDateTime accessTime);
    
    /**
     * 리프레시 토큰 업데이트
     */
    @Modifying
    @Query("UPDATE AuthUserSessionEntity s SET s.refreshToken = :refreshToken, " +
           "s.lastAccessedAt = :accessTime WHERE s.sessionId = :sessionId")
    int updateRefreshToken(@Param("sessionId") String sessionId, 
                           @Param("refreshToken") String refreshToken,
                           @Param("accessTime") LocalDateTime accessTime);
    
    /**
     * 특정 사용자의 모든 세션 삭제
     */
    @Modifying
    @Query("DELETE FROM AuthUserSessionEntity s WHERE s.userId = :userId")
    int deleteAllByUserId(@Param("userId") String userId);
    
    /**
     * 만료된 세션 삭제
     */
    @Modifying
    @Query("DELETE FROM AuthUserSessionEntity s WHERE s.expiresAt < :expirationTime")
    int deleteExpiredSessions(@Param("expirationTime") LocalDateTime expirationTime);
    
    /**
     * 특정 세션 ID로 세션 삭제
     */
    @Modifying
    @Query("DELETE FROM AuthUserSessionEntity s WHERE s.sessionId = :sessionId")
    int deleteBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * IP와 User-Agent로 세션 조회 (보안 검증용)
     */
    Optional<AuthUserSessionEntity> findBySessionIdAndClientIpAndUserAgent(
            String sessionId, String clientIp, String userAgent);
    
    /**
     * 사용자 ID와 리프레시 토큰으로 활성 세션 조회
     */
    Optional<AuthUserSessionEntity> findByUserIdAndRefreshTokenAndIsActiveTrue(String userId, String refreshToken);
    
    /**
     * 사용자 ID와 세션 토큰으로 활성 세션 조회
     */
    Optional<AuthUserSessionEntity> findByUserIdAndSessionTokenAndIsActiveTrue(String userId, String sessionToken);
    
    /**
     * 사용자의 모든 세션 비활성화
     */
    @Modifying
    @Query("UPDATE AuthUserSessionEntity s SET s.isActive = false WHERE s.userId = :userId")
    int deactivateAllUserSessions(@Param("userId") String userId);
}