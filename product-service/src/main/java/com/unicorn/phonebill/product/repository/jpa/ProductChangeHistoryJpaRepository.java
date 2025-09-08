package com.unicorn.phonebill.product.repository.jpa;

import com.unicorn.phonebill.product.domain.ProcessStatus;
import com.unicorn.phonebill.product.repository.entity.ProductChangeHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 상품변경 이력 JPA Repository
 */
@Repository
public interface ProductChangeHistoryJpaRepository extends JpaRepository<ProductChangeHistoryEntity, Long> {

    /**
     * 요청 ID로 이력 조회
     */
    Optional<ProductChangeHistoryEntity> findByRequestId(String requestId);

    /**
     * 회선번호로 이력 조회 (최신순)
     */
    @Query("SELECT h FROM ProductChangeHistoryEntity h " +
           "WHERE h.lineNumber = :lineNumber " +
           "ORDER BY h.requestedAt DESC")
    Page<ProductChangeHistoryEntity> findByLineNumberOrderByRequestedAtDesc(
            @Param("lineNumber") String lineNumber, 
            Pageable pageable);

    /**
     * 고객 ID로 이력 조회 (최신순)
     */
    @Query("SELECT h FROM ProductChangeHistoryEntity h " +
           "WHERE h.customerId = :customerId " +
           "ORDER BY h.requestedAt DESC")
    Page<ProductChangeHistoryEntity> findByCustomerIdOrderByRequestedAtDesc(
            @Param("customerId") String customerId, 
            Pageable pageable);

    /**
     * 처리 상태별 이력 조회
     */
    @Query("SELECT h FROM ProductChangeHistoryEntity h " +
           "WHERE h.processStatus = :status " +
           "ORDER BY h.requestedAt DESC")
    Page<ProductChangeHistoryEntity> findByProcessStatusOrderByRequestedAtDesc(
            @Param("status") ProcessStatus status, 
            Pageable pageable);

    /**
     * 기간별 이력 조회
     */
    @Query("SELECT h FROM ProductChangeHistoryEntity h " +
           "WHERE h.requestedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY h.requestedAt DESC")
    Page<ProductChangeHistoryEntity> findByRequestedAtBetweenOrderByRequestedAtDesc(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * 회선번호와 기간으로 이력 조회
     */
    @Query("SELECT h FROM ProductChangeHistoryEntity h " +
           "WHERE h.lineNumber = :lineNumber " +
           "AND h.requestedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY h.requestedAt DESC")
    Page<ProductChangeHistoryEntity> findByLineNumberAndRequestedAtBetweenOrderByRequestedAtDesc(
            @Param("lineNumber") String lineNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * 처리 중인 요청 조회 (타임아웃 체크용)
     */
    @Query("SELECT h FROM ProductChangeHistoryEntity h " +
           "WHERE h.processStatus IN ('PROCESSING', 'VALIDATED') " +
           "AND h.requestedAt < :timeoutThreshold " +
           "ORDER BY h.requestedAt ASC")
    List<ProductChangeHistoryEntity> findProcessingRequestsOlderThan(
            @Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * 특정 회선번호의 최근 성공한 상품변경 이력 조회
     */
    @Query("SELECT h FROM ProductChangeHistoryEntity h " +
           "WHERE h.lineNumber = :lineNumber " +
           "AND h.processStatus = 'COMPLETED' " +
           "ORDER BY h.processedAt DESC")
    Page<ProductChangeHistoryEntity> findLatestSuccessfulChangeByLineNumber(
            @Param("lineNumber") String lineNumber,
            Pageable pageable);

    /**
     * 특정 기간 동안의 상품변경 통계 조회
     */
    @Query("SELECT h.processStatus, COUNT(h) FROM ProductChangeHistoryEntity h " +
           "WHERE h.requestedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY h.processStatus")
    List<Object[]> getChangeStatisticsByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 현재 상품코드에서 대상 상품코드로의 변경 횟수 조회
     */
    @Query("SELECT COUNT(h) FROM ProductChangeHistoryEntity h " +
           "WHERE h.currentProductCode = :currentProductCode " +
           "AND h.targetProductCode = :targetProductCode " +
           "AND h.processStatus = 'COMPLETED' " +
           "AND h.processedAt >= :fromDate")
    long countSuccessfulChangesByProductCodesSince(
            @Param("currentProductCode") String currentProductCode,
            @Param("targetProductCode") String targetProductCode,
            @Param("fromDate") LocalDateTime fromDate);

    /**
     * 회선별 진행 중인 요청이 있는지 확인
     */
    @Query("SELECT COUNT(h) FROM ProductChangeHistoryEntity h " +
           "WHERE h.lineNumber = :lineNumber " +
           "AND h.processStatus IN ('PROCESSING', 'VALIDATED')")
    long countInProgressRequestsByLineNumber(@Param("lineNumber") String lineNumber);

    /**
     * 요청 ID 존재 여부 확인
     */
    boolean existsByRequestId(String requestId);
}