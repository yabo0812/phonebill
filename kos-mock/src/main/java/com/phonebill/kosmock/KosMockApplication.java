package com.phonebill.kosmock;

import com.phonebill.kosmock.data.MockDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * KOS Mock Service 메인 애플리케이션 클래스
 */
@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class KosMockApplication implements CommandLineRunner {

    private final MockDataService mockDataService;

    public static void main(String[] args) {
        SpringApplication.run(KosMockApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=== KOS Mock Service 시작 ===");
        log.info("Mock 데이터 초기화를 시작합니다...");
        
        mockDataService.initializeMockData();

    }
}