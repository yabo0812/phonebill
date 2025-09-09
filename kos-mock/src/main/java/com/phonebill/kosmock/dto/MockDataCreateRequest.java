package com.phonebill.kosmock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Mock 데이터 생성 요청 DTO
 */
@Data
public class MockDataCreateRequest {
    
    @JsonProperty("customerId")
    @NotBlank(message = "고객 ID는 필수입니다")
    private String customerId;
    
    @JsonProperty("lineNumber")
    @NotBlank(message = "회선번호는 필수입니다")
    @Pattern(regexp = "^010\\d{8}$", message = "회선번호 형식이 올바르지 않습니다 (예: 01012345678)")
    private String lineNumber;
}