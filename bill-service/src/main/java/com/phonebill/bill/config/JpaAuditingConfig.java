package com.phonebill.bill.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 * 
 * BaseTimeEntity의 @CreatedDate, @LastModifiedDate 자동 설정을 위한 구성
 * - 엔티티 저장/수정 시 자동으로 시간 정보 설정
 * - 모든 엔티티의 생성/수정 시간 추적 가능
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-09
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // JPA Auditing 활성화를 위한 설정 클래스
    // 별도의 Bean 정의 없이 @EnableJpaAuditing만으로 충분
}