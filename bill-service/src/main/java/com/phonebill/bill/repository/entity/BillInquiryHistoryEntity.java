package com.phonebill.bill.repository.entity;

import com.phonebill.bill.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 요금조회 이력 엔티티
 * 
 * 요금조회 요청 및 처리 이력을 저장하는 엔티티
 * - 요청 ID를 통한 추적 가능
 * - 처리 상태별 이력 관리
 * - 성능을 위한 인덱스 최적화
 * - 페이징 처리를 위한 정렬 기준 제공
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Entity
@Table(
    name = "bill_inquiry_history",
    indexes = {
        @Index(name = "idx_request_id", columnList = "request_id"),
        @Index(name = "idx_line_number", columnList = "line_number"),
        @Index(name = "idx_request_time", columnList = "request_time"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_line_request_time", columnList = "line_number, request_time")
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillInquiryHistoryEntity extends BaseTimeEntity {

    /**
     * 기본 키 (자동 증가)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 요금조회 요청 ID (고유 식별자)
     */
    @Column(name = "request_id", nullable = false, unique = true, length = 50)
    private String requestId;

    /**
     * 회선번호
     */
    @Column(name = "line_number", nullable = false, length = 15)
    private String lineNumber;

    /**
     * 조회월 (YYYY-MM 형식)
     */
    @Column(name = "inquiry_month", nullable = false, length = 7)
    private String inquiryMonth;

    /**
     * 요청일시
     */
    @Column(name = "request_time", nullable = false)
    private LocalDateTime requestTime;

    /**
     * 처리일시
     */
    @Column(name = "process_time")
    private LocalDateTime processTime;

    /**
     * 처리 상태 (COMPLETED, PROCESSING, FAILED)
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 결과 요약 (성공시 요금제명과 금액, 실패시 오류 메시지)
     */
    @Column(name = "result_summary", length = 500)
    private String resultSummary;

    /**
     * KOS 응답 시간 (성능 모니터링용)
     */
    @Column(name = "kos_response_time_ms")
    private Long kosResponseTimeMs;

    /**
     * 캐시 히트 여부 (성능 모니터링용)
     */
    @Column(name = "cache_hit")
    private Boolean cacheHit;

    /**
     * 오류 코드 (실패시)
     */
    @Column(name = "error_code", length = 50)
    private String errorCode;

    /**
     * 오류 메시지 (실패시)
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    // === Business Methods ===

    /**
     * 상태 업데이트
     * 
     * @param newStatus 새로운 상태
     */
    public void updateStatus(String newStatus) {
        this.status = newStatus;
    }

    /**
     * 처리 시간 업데이트
     * 
     * @param processTime 처리 완료 시간
     */
    public void updateProcessTime(LocalDateTime processTime) {
        this.processTime = processTime;
    }

    /**
     * 결과 요약 업데이트
     * 
     * @param resultSummary 결과 요약
     */
    public void updateResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    /**
     * KOS 응답 시간 설정
     * 
     * @param kosResponseTimeMs KOS 응답 시간 (밀리초)
     */
    public void setKosResponseTime(Long kosResponseTimeMs) {
        this.kosResponseTimeMs = kosResponseTimeMs;
    }

    /**
     * 캐시 히트 여부 설정
     * 
     * @param cacheHit 캐시 히트 여부
     */
    public void setCacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    /**
     * 오류 정보 설정
     * 
     * @param errorCode 오류 코드
     * @param errorMessage 오류 메시지
     */
    public void setErrorInfo(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.status = "FAILED";
    }

    /**
     * 처리 완료로 상태 변경
     * 
     * @param resultSummary 결과 요약
     */
    public void markAsCompleted(String resultSummary) {
        this.status = "COMPLETED";
        this.processTime = LocalDateTime.now();
        this.resultSummary = resultSummary;
    }

    /**
     * 처리 실패로 상태 변경
     * 
     * @param errorCode 오류 코드
     * @param errorMessage 오류 메시지
     */
    public void markAsFailed(String errorCode, String errorMessage) {
        this.status = "FAILED";
        this.processTime = LocalDateTime.now();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.resultSummary = "조회 실패: " + errorMessage;
    }

    /**
     * 처리 중 상태인지 확인
     * 
     * @return 처리 중 상태 여부
     */
    public boolean isProcessing() {
        return "PROCESSING".equals(this.status);
    }

    /**
     * 처리 완료 상태인지 확인
     * 
     * @return 처리 완료 상태 여부
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }

    /**
     * 처리 실패 상태인지 확인
     * 
     * @return 처리 실패 상태 여부
     */
    public boolean isFailed() {
        return "FAILED".equals(this.status);
    }

    /**
     * 캐시에서 조회된 요청인지 확인
     * 
     * @return 캐시 히트 여부
     */
    public boolean isCacheHit() {
        return Boolean.TRUE.equals(this.cacheHit);
    }

    /**
     * 처리 소요 시간 계산 (밀리초)
     * 
     * @return 처리 소요 시간 (밀리초), 처리 중이거나 처리시간이 없으면 null
     */
    public Long getProcessingTimeMs() {
        if (requestTime != null && processTime != null) {
            return java.time.Duration.between(requestTime, processTime).toMillis();
        }
        return null;
    }
}