package com.unicorn.phonebill.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 변경 결과 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeResult {
    private String requestId;
    private String status;
    private String message;
    private String processedAt;
    private String completedAt;
}
