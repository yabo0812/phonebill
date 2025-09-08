package com.phonebill.kosmock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * KOS Mock Application 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class KosMockApplicationTest {

    @Test
    void contextLoads() {
        // Spring Context가 정상적으로 로드되는지 확인
    }
}