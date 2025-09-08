package com.phonebill.user.service;

import com.phonebill.user.dto.*;
import com.phonebill.user.entity.AuthUserEntity;
import com.phonebill.user.entity.AuthPermissionEntity;
import com.phonebill.user.entity.AuthUserPermissionEntity;
import com.phonebill.user.exception.UserNotFoundException;
import com.phonebill.user.repository.AuthUserRepository;
import com.phonebill.user.repository.AuthPermissionRepository;
import com.phonebill.user.repository.AuthUserPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 사용자 관리 서비스
 * 사용자 정보 조회, 권한 관리 등을 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final AuthUserRepository authUserRepository;
    private final AuthPermissionRepository authPermissionRepository;
    private final AuthUserPermissionRepository authUserPermissionRepository;
    
    /**
     * 사용자 정보 조회
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    public UserInfoResponse getUserInfo(String userId) {
        AuthUserEntity user = authUserRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));
        
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .customerId(user.getCustomerId())
                .lineNumber(user.getLineNumber())
                .accountStatus(user.getAccountStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .lastPasswordChangedAt(user.getLastPasswordChangedAt())
                .build();
    }
    
    /**
     * 고객 ID로 사용자 정보 조회
     * @param customerId 고객 ID
     * @return 사용자 정보
     */
    public UserInfoResponse getUserInfoByCustomerId(String customerId) {
        AuthUserEntity user = authUserRepository.findByCustomerId(customerId)
                .orElseThrow(() -> UserNotFoundException.byCustomerId(customerId));
        
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .customerId(user.getCustomerId())
                .lineNumber(user.getLineNumber())
                .accountStatus(user.getAccountStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .lastPasswordChangedAt(user.getLastPasswordChangedAt())
                .build();
    }
    
    /**
     * 회선번호로 사용자 정보 조회
     * @param lineNumber 회선번호
     * @return 사용자 정보
     */
    public UserInfoResponse getUserInfoByLineNumber(String lineNumber) {
        AuthUserEntity user = authUserRepository.findByLineNumber(lineNumber)
                .orElseThrow(() -> UserNotFoundException.byLineNumber(lineNumber));
        
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .customerId(user.getCustomerId())
                .lineNumber(user.getLineNumber())
                .accountStatus(user.getAccountStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .lastPasswordChangedAt(user.getLastPasswordChangedAt())
                .build();
    }
    
    /**
     * 사용자 권한 목록 조회
     * @param userId 사용자 ID
     * @return 권한 목록
     */
    public PermissionsResponse getUserPermissions(String userId) {
        // 사용자 존재 확인
        if (!authUserRepository.existsByUserId(userId)) {
            throw UserNotFoundException.byUserId(userId);
        }
        
        // 사용자가 보유한 권한 코드 목록 조회
        List<String> permissionCodes = authUserPermissionRepository.findPermissionCodesByUserId(userId);
        
        // 권한 코드를 Permission 객체로 변환
        List<PermissionsResponse.Permission> permissions = permissionCodes.stream()
                .map(code -> PermissionsResponse.Permission.builder()
                        .permission(code)
                        .description(getPermissionDescription(code))
                        .granted(true)
                        .build())
                .collect(Collectors.toList());
        
        return PermissionsResponse.builder()
                .userId(userId)
                .permissions(permissions)
                .build();
    }
    
    /**
     * 특정 권한 보유 여부 확인
     * @param request 권한 확인 요청
     * @return 권한 확인 결과
     */
    public PermissionCheckResponse checkPermission(PermissionCheckRequest request) {
        String userId = request.getUserId();
        String permissionCode = request.getPermissionCode();
        
        // 사용자 존재 확인
        if (!authUserRepository.existsByUserId(userId)) {
            return PermissionCheckResponse.builder()
                    .userId(userId)
                    .permissionCode(permissionCode)
                    .hasPermission(false)
                    .message("사용자를 찾을 수 없습니다.")
                    .build();
        }
        
        // 권한 존재 확인
        Optional<AuthPermissionEntity> permissionOpt = 
                authPermissionRepository.findByPermissionCodeAndIsActiveTrue(permissionCode);
        
        if (permissionOpt.isEmpty()) {
            return PermissionCheckResponse.builder()
                    .userId(userId)
                    .permissionCode(permissionCode)
                    .hasPermission(false)
                    .message("존재하지 않는 권한입니다.")
                    .build();
        }
        
        AuthPermissionEntity permission = permissionOpt.get();
        boolean hasPermission = authUserPermissionRepository.hasPermission(userId, permission.getPermissionId());
        
        return PermissionCheckResponse.builder()
                .userId(userId)
                .permissionCode(permissionCode)
                .hasPermission(hasPermission)
                .message(hasPermission ? "권한이 있습니다." : "권한이 없습니다.")
                .build();
    }
    
    /**
     * 서비스별 사용자 권한 조회
     * @param userId 사용자 ID
     * @param serviceCode 서비스 코드
     * @return 서비스별 권한 목록
     */
    public List<String> getUserPermissionsByService(String userId, String serviceCode) {
        // 사용자 존재 확인
        if (!authUserRepository.existsByUserId(userId)) {
            throw UserNotFoundException.byUserId(userId);
        }
        
        // 서비스별 사용자 권한 조회
        List<AuthUserPermissionEntity> userPermissions = 
                authUserPermissionRepository.findUserPermissionsByService(userId, serviceCode);
        
        // 권한 코드 목록으로 변환
        return userPermissions.stream()
                .map(up -> {
                    // 권한 정보 조회
                    Optional<AuthPermissionEntity> permissionOpt = 
                            authPermissionRepository.findById(up.getPermissionId());
                    return permissionOpt.map(AuthPermissionEntity::getPermissionCode).orElse(null);
                })
                .filter(permissionCode -> permissionCode != null)
                .collect(Collectors.toList());
    }
    
    /**
     * 권한 부여
     * @param userId 사용자 ID
     * @param permissionCode 권한 코드
     * @param grantedBy 권한 부여자
     */
    @Transactional
    public void grantPermission(String userId, String permissionCode, String grantedBy) {
        // 사용자 존재 확인
        if (!authUserRepository.existsByUserId(userId)) {
            throw UserNotFoundException.byUserId(userId);
        }
        
        // 권한 조회
        AuthPermissionEntity permission = authPermissionRepository
                .findByPermissionCodeAndIsActiveTrue(permissionCode)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 권한입니다: " + permissionCode));
        
        // 기존 권한 관계 확인
        Optional<AuthUserPermissionEntity> existingPermission = 
                authUserPermissionRepository.findByUserIdAndPermissionId(userId, permission.getPermissionId());
        
        if (existingPermission.isPresent()) {
            // 기존 관계가 있으면 업데이트
            authUserPermissionRepository.grantPermission(userId, permission.getPermissionId(), grantedBy);
        } else {
            // 새로운 권한 관계 생성
            AuthUserPermissionEntity userPermission = AuthUserPermissionEntity.builder()
                    .userId(userId)
                    .permissionId(permission.getPermissionId())
                    .granted(true)
                    .grantedBy(grantedBy)
                    .build();
            
            authUserPermissionRepository.save(userPermission);
        }
        
        log.info("권한 부여 완료: userId={}, permissionCode={}, grantedBy={}", 
                userId, permissionCode, grantedBy);
    }
    
    /**
     * 권한 철회
     * @param userId 사용자 ID
     * @param permissionCode 권한 코드
     */
    @Transactional
    public void revokePermission(String userId, String permissionCode) {
        // 사용자 존재 확인
        if (!authUserRepository.existsByUserId(userId)) {
            throw UserNotFoundException.byUserId(userId);
        }
        
        // 권한 조회
        AuthPermissionEntity permission = authPermissionRepository
                .findByPermissionCodeAndIsActiveTrue(permissionCode)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 권한입니다: " + permissionCode));
        
        // 권한 철회
        authUserPermissionRepository.revokePermission(userId, permission.getPermissionId());
        
        log.info("권한 철회 완료: userId={}, permissionCode={}", userId, permissionCode);
    }
    
    /**
     * 사용자 ID 존재 여부 확인
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    public boolean existsUserId(String userId) {
        return authUserRepository.existsByUserId(userId);
    }
    
    /**
     * 고객 ID 존재 여부 확인
     * @param customerId 고객 ID
     * @return 존재 여부
     */
    public boolean existsCustomerId(String customerId) {
        return authUserRepository.existsByCustomerId(customerId);
    }
    
    /**
     * 계정 상태 확인
     * @param userId 사용자 ID
     * @return 계정 상태 정보
     */
    public AuthUserEntity.AccountStatus getAccountStatus(String userId) {
        AuthUserEntity user = authUserRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));
        
        return user.getAccountStatus();
    }
    
    /**
     * 계정 활성 상태 확인
     * @param userId 사용자 ID
     * @return 활성 상태 여부
     */
    public boolean isAccountActive(String userId) {
        AuthUserEntity user = authUserRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));
        
        return user.isAccountActive();
    }
    
    /**
     * 계정 잠금 해제 (관리자용)
     * @param userId 사용자 ID
     */
    @Transactional
    public void unlockAccount(String userId) {
        AuthUserEntity user = authUserRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));
        
        user.unlockAccount();
        authUserRepository.save(user);
        
        log.info("계정 잠금 해제: userId={}", userId);
    }
    
    /**
     * 권한 코드에 대한 설명 조회
     * @param permissionCode 권한 코드
     * @return 권한 설명
     */
    private String getPermissionDescription(String permissionCode) {
        Optional<AuthPermissionEntity> permission = 
                authPermissionRepository.findByPermissionCodeAndIsActiveTrue(permissionCode);
        return permission.map(AuthPermissionEntity::getPermissionDescription).orElse("설명 없음");
    }
}