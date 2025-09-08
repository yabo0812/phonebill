package com.unicorn.phonebill.product.repository;

import com.unicorn.phonebill.product.domain.ProductChangeHistory;
import com.unicorn.phonebill.product.domain.ProcessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 상품변경 이력 Repository 인터페이스
 */
public interface ProductChangeHistoryRepository {

    /**
     * 상품변경 이력 저장
     */
    ProductChangeHistory save(ProductChangeHistory history);

    /**
     * 요청 ID로 이력 조회
     */
    Optional<ProductChangeHistory> findByRequestId(String requestId);

    /**
     * 회선번호로 이력 조회 (페이징)
     */
    Page<ProductChangeHistory> findByLineNumber(String lineNumber, Pageable pageable);

    /**
     * 고객 ID로 이력 조회 (페이징)
     */
    Page<ProductChangeHistory> findByCustomerId(String customerId, Pageable pageable);

    /**
     * 처리 상태별 이력 조회 (페이징)
     */
    Page<ProductChangeHistory> findByProcessStatus(ProcessStatus status, Pageable pageable);

    /**
     * 기간별 이력 조회 (페이징)
     */
    Page<ProductChangeHistory> findByPeriod(
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable);

    /**
     * 회선번호와 기간으로 이력 조회 (페이징)
     */
    Page<ProductChangeHistory> findByLineNumberAndPeriod(
            String lineNumber,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    /**
     * 처리 중인 요청 조회 (타임아웃 체크용)
     */
    List<ProductChangeHistory> findProcessingRequestsOlderThan(LocalDateTime timeoutThreshold);

    /**
     * 특정 회선번호의 최근 성공한 상품변경 이력 조회
     */
    Optional<ProductChangeHistory> findLatestSuccessfulChangeByLineNumber(String lineNumber);

    /**
     * 상품변경 통계 조회 (특정 기간)
     */
    List<Object[]> getChangeStatisticsByPeriod(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 상품 간 변경 횟수 조회
     */
    long countSuccessfulChangesByProductCodesSince(
            String currentProductCode,
            String targetProductCode,
            LocalDateTime fromDate);

    /**
     * 회선별 진행 중인 요청 개수 조회
     */
    long countInProgressRequestsByLineNumber(String lineNumber);

    /**
     * 요청 ID 존재 여부 확인
     */
    boolean existsByRequestId(String requestId);

    /**
     * 이력 삭제 (관리용)
     */
    void deleteById(Long id);

    /**
     * 전체 개수 조회
     */
    long count();
}