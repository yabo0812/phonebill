package com.phonebill.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 설정
 * JPA Auditing과 Repository 설정을 제공합니다.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.phonebill")
public class JpaConfig {
}
