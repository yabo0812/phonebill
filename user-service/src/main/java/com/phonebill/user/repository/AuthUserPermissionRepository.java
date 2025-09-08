package com.phonebill.user.repository;

import com.phonebill.user.entity.AuthUserPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 권한 Repository
 */
@Repository
public interface AuthUserPermissionRepository extends JpaRepository<AuthUserPermissionEntity, Long> {
    
    /**
     * 사용자의 모든 권한 조회
     */
    List<AuthUserPermissionEntity> findByUserId(String userId);
    
    /**
     * 사용자의 부여된 권한만 조회
     */
    List<AuthUserPermissionEntity> findByUserIdAndGrantedTrue(String userId);
    
    /**
     * 사용자의 특정 권한 조회
     */
    Optional<AuthUserPermissionEntity> findByUserIdAndPermissionId(String userId, Long permissionId);
    
    /**
     * 사용자의 특정 권한 보유 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(up) > 0 THEN true ELSE false END " +
           "FROM AuthUserPermissionEntity up " +
           "WHERE up.userId = :userId AND up.permissionId = :permissionId AND up.granted = true")
    boolean hasPermission(@Param("userId") String userId, @Param("permissionId") Long permissionId);
    
    /**
     * 서비스별 사용자 권한 조회
     */
    @Query("SELECT up FROM AuthUserPermissionEntity up " +
           "JOIN AuthPermissionEntity p ON up.permissionId = p.permissionId " +
           "WHERE up.userId = :userId AND p.serviceCode = :serviceCode AND up.granted = true")
    List<AuthUserPermissionEntity> findUserPermissionsByService(@Param("userId") String userId, 
                                                                 @Param("serviceCode") String serviceCode);
    
    /**
     * 권한 코드로 사용자 권한 조회
     */
    @Query("SELECT up FROM AuthUserPermissionEntity up " +
           "JOIN AuthPermissionEntity p ON up.permissionId = p.permissionId " +
           "WHERE up.userId = :userId AND p.permissionCode = :permissionCode AND up.granted = true")
    Optional<AuthUserPermissionEntity> findByUserIdAndPermissionCode(@Param("userId") String userId, 
                                                                      @Param("permissionCode") String permissionCode);
    
    /**
     * 사용자가 보유한 권한 코드 목록 조회
     */
    @Query("SELECT p.permissionCode FROM AuthUserPermissionEntity up " +
           "JOIN AuthPermissionEntity p ON up.permissionId = p.permissionId " +
           "WHERE up.userId = :userId AND up.granted = true AND p.isActive = true")
    List<String> findPermissionCodesByUserId(@Param("userId") String userId);
    
    /**
     * 권한 부여
     */
    @Modifying
    @Query("UPDATE AuthUserPermissionEntity up SET up.granted = true, up.grantedBy = :grantedBy " +
           "WHERE up.userId = :userId AND up.permissionId = :permissionId")
    int grantPermission(@Param("userId") String userId, 
                        @Param("permissionId") Long permissionId, 
                        @Param("grantedBy") String grantedBy);
    
    /**
     * 권한 철회
     */
    @Modifying
    @Query("UPDATE AuthUserPermissionEntity up SET up.granted = false " +
           "WHERE up.userId = :userId AND up.permissionId = :permissionId")
    int revokePermission(@Param("userId") String userId, @Param("permissionId") Long permissionId);
    
    /**
     * 사용자의 모든 권한 철회
     */
    @Modifying
    @Query("UPDATE AuthUserPermissionEntity up SET up.granted = false WHERE up.userId = :userId")
    int revokeAllPermissions(@Param("userId") String userId);
    
    /**
     * 사용자의 모든 권한 삭제
     */
    @Modifying
    @Query("DELETE FROM AuthUserPermissionEntity up WHERE up.userId = :userId")
    int deleteAllByUserId(@Param("userId") String userId);
    
    /**
     * 특정 권한을 가진 사용자 수 조회
     */
    @Query("SELECT COUNT(DISTINCT up.userId) FROM AuthUserPermissionEntity up " +
           "WHERE up.permissionId = :permissionId AND up.granted = true")
    long countUsersWithPermission(@Param("permissionId") Long permissionId);
}