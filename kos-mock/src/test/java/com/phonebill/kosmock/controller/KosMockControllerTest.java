package com.phonebill.kosmock.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebill.kosmock.dto.KosBillInquiryRequest;
import com.phonebill.kosmock.dto.KosProductChangeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * KOS Mock Controller 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KosMockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("서비스 상태 체크 API 테스트")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/kos/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resultCode").value("0000"));
    }

    @Test
    @DisplayName("요금 조회 API 성공 테스트")
    void inquireBill_Success() throws Exception {
        KosBillInquiryRequest request = new KosBillInquiryRequest();
        request.setLineNumber("01012345678");
        request.setBillingMonth("202501");
        request.setRequestId("TEST_REQ_001");
        request.setRequestorId("TEST_SERVICE");

        mockMvc.perform(post("/api/v1/kos/bill/inquiry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("요금 조회 API 입력값 검증 실패 테스트")
    void inquireBill_ValidationFailure() throws Exception {
        KosBillInquiryRequest request = new KosBillInquiryRequest();
        // 필수값 누락
        request.setBillingMonth("202501");

        mockMvc.perform(post("/api/v1/kos/bill/inquiry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("상품 변경 API 성공 테스트")
    void changeProduct_Success() throws Exception {
        KosProductChangeRequest request = new KosProductChangeRequest();
        request.setLineNumber("01012345678");
        request.setCurrentProductCode("LTE-BASIC-001");
        request.setTargetProductCode("5G-PREMIUM-001");
        request.setRequestId("TEST_REQ_002");
        request.setRequestorId("TEST_SERVICE");
        request.setChangeReason("테스트 상품 변경");

        mockMvc.perform(post("/api/v1/kos/product/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Mock 설정 조회 API 테스트")
    void getMockConfig() throws Exception {
        mockMvc.perform(get("/api/v1/kos/mock/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resultCode").value("0000"));
    }
}