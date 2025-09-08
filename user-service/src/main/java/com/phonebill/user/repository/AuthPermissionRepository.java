package com.phonebill.user.repository;

import com.phonebill.user.entity.AuthPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 권한 정의 Repository
 */
@Repository
public interface AuthPermissionRepository extends JpaRepository<AuthPermissionEntity, Long> {
    
    /**
     * 서비스 코드로 권한 목록 조회
     */
    List<AuthPermissionEntity> findByServiceCodeAndIsActiveTrue(String serviceCode);
    
    /**
     * 권한 코드로 권한 조회
     */
    Optional<AuthPermissionEntity> findByPermissionCodeAndIsActiveTrue(String permissionCode);
    
    /**
     * 서비스 코드와 권한 코드로 권한 조회
     */
    Optional<AuthPermissionEntity> findByServiceCodeAndPermissionCodeAndIsActiveTrue(
            String serviceCode, String permissionCode);
    
    /**
     * 모든 활성 권한 조회
     */
    List<AuthPermissionEntity> findByIsActiveTrue();
    
    /**
     * 서비스 코드 존재 여부 확인
     */
    boolean existsByServiceCodeAndIsActiveTrue(String serviceCode);
    
    /**
     * 권한 코드 존재 여부 확인
     */
    boolean existsByPermissionCodeAndIsActiveTrue(String permissionCode);
    
    /**
     * 서비스별 활성 권한 수 조회
     */
    @Query("SELECT COUNT(p) FROM AuthPermissionEntity p " +
           "WHERE p.serviceCode = :serviceCode AND p.isActive = true")
    long countActivePermissionsByServiceCode(@Param("serviceCode") String serviceCode);
}