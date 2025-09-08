package com.phonebill.user.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 권한 정의 엔티티
 * 시스템 내 권한 정보를 관리
 */
@Entity
@Table(name = "auth_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuthPermissionEntity extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;
    
    @Column(name = "service_code", nullable = false, length = 30)
    private String serviceCode;
    
    @Column(name = "permission_code", nullable = false, length = 50)
    private String permissionCode;
    
    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;
    
    @Column(name = "permission_description", columnDefinition = "TEXT")
    private String permissionDescription;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    /**
     * 권한 활성화
     */
    public void activate() {
        this.isActive = true;
    }
    
    /**
     * 권한 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
}