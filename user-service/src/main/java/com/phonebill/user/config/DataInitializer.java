package com.phonebill.user.config;

import com.phonebill.user.entity.AuthPermissionEntity;
import com.phonebill.user.enums.PermissionCode;
import com.phonebill.user.repository.AuthPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 시작 시 기본 데이터 초기화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final AuthPermissionRepository authPermissionRepository;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializePermissions();
    }
    
    /**
     * 기본 권한 데이터 초기화
     */
    private void initializePermissions() {
        log.info("권한 데이터 초기화 시작");
        
        for (PermissionCode permissionCode : PermissionCode.values()) {
            // 이미 존재하는 권한인지 확인
            if (!authPermissionRepository.existsByPermissionCodeAndIsActiveTrue(permissionCode.getCode())) {
                AuthPermissionEntity permission = AuthPermissionEntity.builder()
                        .serviceCode("USER_SERVICE")
                        .permissionCode(permissionCode.getCode())
                        .permissionName(permissionCode.getCode())
                        .permissionDescription(permissionCode.getDescription())
                        .isActive(true)
                        .build();
                        
                authPermissionRepository.save(permission);
                log.info("권한 생성: {}", permissionCode.getCode());
            } else {
                log.debug("권한 이미 존재: {}", permissionCode.getCode());
            }
        }
        
        log.info("권한 데이터 초기화 완료");
    }
}