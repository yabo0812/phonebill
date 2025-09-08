package com.unicorn.phonebill.product.repository.entity;

import com.unicorn.phonebill.product.domain.ProductChangeHistory;
import com.unicorn.phonebill.product.domain.ProcessStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 상품변경 이력 엔티티
 * 모든 상품변경 요청 및 처리 이력을 관리
 */
@Entity
@Table(name = "pc_product_change_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductChangeHistoryEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 50)
    private String requestId;

    @Column(name = "line_number", nullable = false, length = 20)
    private String lineNumber;

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "current_product_code", nullable = false, length = 20)
    private String currentProductCode;

    @Column(name = "target_product_code", nullable = false, length = 20)
    private String targetProductCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false, length = 20)
    private ProcessStatus processStatus;

    @Column(name = "validation_result", columnDefinition = "TEXT")
    private String validationResult;

    @Column(name = "process_message", columnDefinition = "TEXT")
    private String processMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "kos_request_data", columnDefinition = "jsonb")
    private Map<String, Object> kosRequestData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "kos_response_data", columnDefinition = "jsonb")
    private Map<String, Object> kosResponseData;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Builder
    public ProductChangeHistoryEntity(
            String requestId,
            String lineNumber,
            String customerId,
            String currentProductCode,
            String targetProductCode,
            ProcessStatus processStatus,
            String validationResult,
            String processMessage,
            Map<String, Object> kosRequestData,
            Map<String, Object> kosResponseData,
            LocalDateTime requestedAt,
            LocalDateTime validatedAt,
            LocalDateTime processedAt) {
        this.requestId = requestId;
        this.lineNumber = lineNumber;
        this.customerId = customerId;
        this.currentProductCode = currentProductCode;
        this.targetProductCode = targetProductCode;
        this.processStatus = processStatus != null ? processStatus : ProcessStatus.REQUESTED;
        this.validationResult = validationResult;
        this.processMessage = processMessage;
        this.kosRequestData = kosRequestData;
        this.kosResponseData = kosResponseData;
        this.requestedAt = requestedAt != null ? requestedAt : LocalDateTime.now();
        this.validatedAt = validatedAt;
        this.processedAt = processedAt;
    }

    /**
     * 도메인 모델로 변환
     */
    public ProductChangeHistory toDomain() {
        return ProductChangeHistory.builder()
                .id(this.id)
                .requestId(this.requestId)
                .lineNumber(this.lineNumber)
                .customerId(this.customerId)
                .currentProductCode(this.currentProductCode)
                .targetProductCode(this.targetProductCode)
                .processStatus(this.processStatus)
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
     * 도메인 모델에서 엔티티로 변환
     */
    public static ProductChangeHistoryEntity fromDomain(ProductChangeHistory domain) {
        return ProductChangeHistoryEntity.builder()
                .requestId(domain.getRequestId())
                .lineNumber(domain.getLineNumber())
                .customerId(domain.getCustomerId())
                .currentProductCode(domain.getCurrentProductCode())
                .targetProductCode(domain.getTargetProductCode())
                .processStatus(domain.getProcessStatus())
                .validationResult(domain.getValidationResult())
                .processMessage(domain.getProcessMessage())
                .kosRequestData(domain.getKosRequestData())
                .kosResponseData(domain.getKosResponseData())
                .requestedAt(domain.getRequestedAt())
                .validatedAt(domain.getValidatedAt())
                .processedAt(domain.getProcessedAt())
                .build();
    }

    /**
     * 상태를 완료로 변경
     */
    public void markAsCompleted(String message, Map<String, Object> kosResponseData) {
        this.processStatus = ProcessStatus.COMPLETED;
        this.processMessage = message;
        this.kosResponseData = kosResponseData;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 상태를 실패로 변경
     */
    public void markAsFailed(String message) {
        this.processStatus = ProcessStatus.FAILED;
        this.processMessage = message;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 검증 완료로 상태 변경
     */
    public void markAsValidated(String validationResult) {
        this.processStatus = ProcessStatus.VALIDATED;
        this.validationResult = validationResult;
        this.validatedAt = LocalDateTime.now();
    }

    /**
     * 처리 중으로 상태 변경
     */
    public void markAsProcessing() {
        this.processStatus = ProcessStatus.PROCESSING;
    }

    /**
     * KOS 요청 데이터 설정
     */
    public void setKosRequestData(Map<String, Object> kosRequestData) {
        this.kosRequestData = kosRequestData;
    }

    /**
     * 처리 메시지 업데이트
     */
    public void updateProcessMessage(String message) {
        this.processMessage = message;
    }
}