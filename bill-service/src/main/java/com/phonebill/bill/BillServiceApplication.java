package com.phonebill.bill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Bill Service 메인 애플리케이션 클래스
 * 
 * 통신요금 조회 서비스의 메인 진입점
 * - 요금조회 메뉴 제공
 * - KOS 시스템 연동을 통한 요금 데이터 조회
 * - Redis 캐싱을 통한 성능 최적화
 * - Circuit Breaker를 통한 외부 시스템 장애 격리
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class BillServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillServiceApplication.class, args);
    }
}