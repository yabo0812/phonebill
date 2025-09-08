package com.unicorn.phonebill.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 검증 결과 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private boolean isValid;
    private String message;
    private List<String> errors;
    private String validationCode;
}
