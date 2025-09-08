package com.unicorn.phonebill.product.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 상품변경 이력 도메인 모델
 */
@Getter
@Builder
public class ProductChangeHistory {
    
    private final Long id;
    private final String requestId;
    private final String lineNumber;
    private final String customerId;
    private final String currentProductCode;
    private final String targetProductCode;
    private final ProcessStatus processStatus;
    private final String validationResult;
    private final String processMessage;
    private final Map<String, Object> kosRequestData;
    private final Map<String, Object> kosResponseData;
    private final LocalDateTime requestedAt;
    private final LocalDateTime validatedAt;
    private final LocalDateTime processedAt;
    private final Long version;

    /**
     * 완료 상태로 변경된 새 인스턴스 생성
     */
    public ProductChangeHistory markAsCompleted(String message, Map<String, Object> kosResponseData) {
        return ProductChangeHistory.builder()
                .id(this.id)
                .requestId(this.requestId)
                .lineNumber(this.lineNumber)
                .customerId(this.customerId)
                .currentProductCode(this.currentProductCode)
                .targetProductCode(this.targetProductCode)
                .processStatus(ProcessStatus.COMPLETED)
                .validationResult(this.validationResult)
                .processMessage(message)
                .kosRequestData(this.kosRequestData)
                .kosResponseData(kosResponseData)
                .requestedAt(this.requestedAt)
                .validatedAt(this.validatedAt)
                .processedAt(LocalDateTime.now())
                .version(this.version)
                .build();
    }

    /**
     * 실패 상태로 변경된 새 인스턴스 생성
     */
    public ProductChangeHistory markAsFailed(String message) {
        return ProductChangeHistory.builder()
                .id(this.id)
                .requestId(this.requestId)
                .lineNumber(this.lineNumber)
                .customerId(this.customerId)
                .currentProductCode(this.currentProductCode)
                .targetProductCode(this.targetProductCode)
                .processStatus(ProcessStatus.FAILED)
                .validationResult(this.validationResult)
                .processMessage(message)
                .kosRequestData(this.kosRequestData)
                .kosResponseData(this.kosResponseData)
                .requestedAt(this.requestedAt)
                .validatedAt(this.validatedAt)
                .processedAt(LocalDateTime.now())
                .version(this.version)
                .build();
    }

    /**
     * 실패 상태로 변경된 새 인스턴스 생성 (오버로딩)
     */
    public ProductChangeHistory markAsFailed(String resultCode, String failureReason) {
        return ProductChangeHistory.builder()
                .id(this.id)
                .requestId(this.requestId)
                .lineNumber(this.lineNumber)
                .customerId(this.customerId)
                .currentProductCode(this.currentProductCode)
                .targetProductCode(this.targetProductCode)
                .processStatus(ProcessStatus.FAILED)
                .validationResult(this.validationResult)
                .processMessage(resultCode + ": " + failureReason)
                .kosRequestData(this.kosRequestData)
                .kosResponseData(this.kosResponseData)
                .requestedAt(this.requestedAt)
                .validatedAt(this.validatedAt)
                .processedAt(LocalDateTime.now())
                .version(this.version)
                .build();
    }

    /**
     * 검증 완료 상태로 변경된 새 인스턴스 생성
     */
    public ProductChangeHistory markAsValidated(String validationResult) {
        return ProductChangeHistory.builder()
                .id(this.id)
                .requestId(this.requestId)
                .lineNumber(this.lineNumber)
                .customerId(this.customerId)
                .currentProductCode(this.currentProductCode)
                .targetProductCode(this.targetProductCode)
                .processStatus(ProcessStatus.VALIDATED)
                .validationResult(validationResult)
                .processMessage(this.processMessage)
                .kosRequestData(this.kosRequestData)
                .kosResponseData(this.kosResponseData)
                .requestedAt(this.requestedAt)
                .validatedAt(LocalDateTime.now())
                .processedAt(this.processedAt)
                .version(this.version)
                .build();
    }

    /**
     * 처리 중 상태로 변경된 새 인스턴스 생성
     */
    public ProductChangeHistory markAsProcessing() {
        return ProductChangeHistory.builder()
                .id(this.id)
                .requestId(this.requestId)
                .lineNumber(this.lineNumber)
                .customerId(this.customerId)
                .currentProductCode(this.currentProductCode)
                .targetProductCode(this.targetProductCode)
                .processStatus(ProcessStatus.PROCESSING)
                .validationResult(this.validationResult)
                .processMessage(this.processMessage)
                .kosRequestData(this.kosRequestData)
                .kosResponseData(this.kosResponseData)
                .requestedAt(this.requestedAt)
                .validatedAt(this.validatedAt)
                .processedAt(this.processedAt)
                .version(this.version)
                .build();
    }

    /**
     * 처리가 완료된 상태인지 확인
     */
    public boolean isFinished() {
        return processStatus != null && processStatus.isFinished();
    }

    /**
     * 성공적으로 완료된 상태인지 확인
     */
    public boolean isSuccessful() {
        return processStatus != null && processStatus.isSuccessful();
    }

    /**
     * 처리 중인 상태인지 확인
     */
    public boolean isInProgress() {
        return processStatus != null && processStatus.isInProgress();
    }

    /**
     * 새로운 상품변경 이력 생성 (팩토리 메소드)
     */
    public static ProductChangeHistory createNew(
            String requestId,
            String lineNumber,
            String customerId,
            String currentProductCode,
            String targetProductCode) {
        
        return ProductChangeHistory.builder()
                .requestId(requestId)
                .lineNumber(lineNumber)
                .customerId(customerId)
                .currentProductCode(currentProductCode)
                .targetProductCode(targetProductCode)
                .processStatus(ProcessStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 결과 코드 추출 (processMessage에서)
     */
    public String getResultCode() {
        if (processMessage != null && processMessage.contains(":")) {
            return processMessage.split(":")[0].trim();
        }
        return processStatus != null ? processStatus.name() : "UNKNOWN";
    }

    /**
     * 결과 메시지 추출 (processMessage에서)
     */
    public String getResultMessage() {
        if (processMessage != null && processMessage.contains(":")) {
            String[] parts = processMessage.split(":", 2);
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }
        return processMessage != null ? processMessage : "처리 메시지가 없습니다.";
    }

    /**
     * 실패 사유 추출 (실패 상태일 때의 processMessage)
     */
    public String getFailureReason() {
        if (processStatus == ProcessStatus.FAILED) {
            return getResultMessage();
        }
        return null;
    }
}