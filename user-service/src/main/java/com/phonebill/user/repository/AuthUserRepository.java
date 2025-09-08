package com.phonebill.user.repository;

import com.phonebill.user.entity.AuthUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 사용자 계정 Repository
 */
@Repository
public interface AuthUserRepository extends JpaRepository<AuthUserEntity, String> {
    
    /**
     * 고객 ID로 사용자 조회
     */
    Optional<AuthUserEntity> findByCustomerId(String customerId);
    
    /**
     * 회선번호로 사용자 조회
     */
    Optional<AuthUserEntity> findByLineNumber(String lineNumber);
    
    /**
     * 활성 상태인 사용자만 조회
     */
    Optional<AuthUserEntity> findByUserIdAndAccountStatus(String userId, AuthUserEntity.AccountStatus status);
    
    /**
     * 사용자 ID 존재 여부 확인
     */
    boolean existsByUserId(String userId);
    
    /**
     * 고객 ID 존재 여부 확인
     */
    boolean existsByCustomerId(String customerId);
    
    /**
     * 로그인 실패 카운트 증가
     */
    @Modifying
    @Query("UPDATE AuthUserEntity u SET u.failedLoginCount = u.failedLoginCount + 1, " +
           "u.lastFailedLoginAt = :failedTime WHERE u.userId = :userId")
    int incrementFailedLoginCount(@Param("userId") String userId, 
                                  @Param("failedTime") LocalDateTime failedTime);
    
    /**
     * 로그인 실패 카운트 초기화
     */
    @Modifying
    @Query("UPDATE AuthUserEntity u SET u.failedLoginCount = 0, " +
           "u.lastFailedLoginAt = null WHERE u.userId = :userId")
    int resetFailedLoginCount(@Param("userId") String userId);
    
    /**
     * 계정 잠금 설정
     */
    @Modifying
    @Query("UPDATE AuthUserEntity u SET u.accountStatus = 'LOCKED', " +
           "u.accountLockedUntil = :lockedUntil WHERE u.userId = :userId")
    int lockAccount(@Param("userId") String userId, 
                    @Param("lockedUntil") LocalDateTime lockedUntil);
    
    /**
     * 계정 잠금 해제
     */
    @Modifying
    @Query("UPDATE AuthUserEntity u SET u.accountStatus = 'ACTIVE', " +
           "u.accountLockedUntil = null, u.failedLoginCount = 0, " +
           "u.lastFailedLoginAt = null WHERE u.userId = :userId")
    int unlockAccount(@Param("userId") String userId);
    
    /**
     * 마지막 로그인 시간 업데이트
     */
    @Modifying
    @Query("UPDATE AuthUserEntity u SET u.lastLoginAt = :loginTime, " +
           "u.failedLoginCount = 0, u.lastFailedLoginAt = null WHERE u.userId = :userId")
    int updateLastLoginTime(@Param("userId") String userId, 
                            @Param("loginTime") LocalDateTime loginTime);
    
    /**
     * 비밀번호 업데이트
     */
    @Modifying
    @Query("UPDATE AuthUserEntity u SET u.passwordHash = :passwordHash, " +
           "u.passwordSalt = :passwordSalt, u.lastPasswordChangedAt = :changedTime " +
           "WHERE u.userId = :userId")
    int updatePassword(@Param("userId") String userId, 
                       @Param("passwordHash") String passwordHash,
                       @Param("passwordSalt") String passwordSalt,
                       @Param("changedTime") LocalDateTime changedTime);
    
    /**
     * 잠금 해제 시간이 지난 계정들의 잠금 자동 해제
     */
    @Modifying
    @Query("UPDATE AuthUserEntity u SET u.accountStatus = 'ACTIVE', " +
           "u.accountLockedUntil = null, u.failedLoginCount = 0, " +
           "u.lastFailedLoginAt = null " +
           "WHERE u.accountStatus = 'LOCKED' AND u.accountLockedUntil < :currentTime")
    int unlockExpiredAccounts(@Param("currentTime") LocalDateTime currentTime);
}