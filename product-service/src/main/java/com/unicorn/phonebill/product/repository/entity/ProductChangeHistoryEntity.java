package com.unicorn.phonebill.product.repository.entity;

import com.unicorn.phonebill.product.domain.ProductChangeHistory;
import com.unicorn.phonebill.product.domain.ProcessStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상품변경 이력 엔티티 (실제 DB 스키마에 맞춘 버전)
 */
@Entity
@Table(name = "pc_product_change_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductChangeHistoryEntity extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 100)
    private String id;

    @Column(name = "line_number", nullable = false, length = 20)
    private String lineNumber;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(name = "old_product_code", length = 50)
    private String currentProductCode;

    @Column(name = "new_product_code", nullable = false, length = 50)
    private String targetProductCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_status", length = 20)
    private ProcessStatus processStatus;

    @Column(name = "change_reason", length = 255)
    private String changeReason;

    @Column(name = "change_method", length = 50)
    private String changeMethod;

    @Column(name = "request_time", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approval_time")
    private LocalDateTime approvalTime;

    @Column(name = "completion_time")
    private LocalDateTime completionTime;

    @Column(name = "approver_id", length = 50)
    private String approverId;

    @Column(name = "processor_id", length = 50)
    private String processorId;

    @Column(name = "kos_request_id", length = 100)
    private String kosRequestId;

    @Column(name = "kos_response_code", length = 20)
    private String kosResponseCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Builder
    public ProductChangeHistoryEntity(
            String id,
            String lineNumber,
            String customerId,
            String currentProductCode,
            String targetProductCode,
            ProcessStatus processStatus,
            String changeReason,
            String changeMethod,
            LocalDateTime requestedAt) {
        this.id = id;
        this.lineNumber = lineNumber;
        this.customerId = customerId;
        this.currentProductCode = currentProductCode;
        this.targetProductCode = targetProductCode;
        this.processStatus = processStatus != null ? processStatus : ProcessStatus.REQUESTED;
        this.changeReason = changeReason;
        this.changeMethod = changeMethod != null ? changeMethod : "API";
        this.requestedAt = requestedAt != null ? requestedAt : LocalDateTime.now();
        this.retryCount = 0;
    }

    /**
     * 도메인 모델로 변환 (간소화)
     */
    public ProductChangeHistory toDomain() {
        return ProductChangeHistory.builder()
                .id(null) // Long type을 위해 null 처리
                .requestId(this.id)
                .lineNumber(this.lineNumber)
                .customerId(this.customerId)
                .currentProductCode(this.currentProductCode)
                .targetProductCode(this.targetProductCode)
                .processStatus(this.processStatus)
                .requestedAt(this.requestedAt)
                .build();
    }

    /**
     * 도메인 모델에서 엔티티로 변환
     */
    public static ProductChangeHistoryEntity fromDomain(ProductChangeHistory domain) {
        return ProductChangeHistoryEntity.builder()
                .id(UUID.randomUUID().toString()) // 새로운 UUID 생성
                .lineNumber(domain.getLineNumber())
                .customerId(domain.getCustomerId())
                .currentProductCode(domain.getCurrentProductCode())
                .targetProductCode(domain.getTargetProductCode())
                .processStatus(domain.getProcessStatus())
                .requestedAt(domain.getRequestedAt())
                .build();
    }

    /**
     * 상태를 완료로 변경
     */
    public void markAsCompleted(String message) {
        this.processStatus = ProcessStatus.COMPLETED;
        this.completionTime = LocalDateTime.now();
    }

    /**
     * 상태를 실패로 변경
     */
    public void markAsFailed(String message) {
        this.processStatus = ProcessStatus.FAILED;
        this.errorMessage = message;
        this.completionTime = LocalDateTime.now();
    }

    /**
     * 상태를 처리중으로 변경
     */
    public void markAsProcessing() {
        this.processStatus = ProcessStatus.PROCESSING;
    }
}