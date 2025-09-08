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
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
})
@EnableCaching
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
        
        log.info("KOS Mock Service가 성공적으로 시작되었습니다.");
        log.info("Swagger UI: http://localhost:8080/kos-mock/swagger-ui.html");
        log.info("Health Check: http://localhost:8080/kos-mock/actuator/health");
    }
}