package com.phonebill.bill.repository;

import com.phonebill.bill.repository.entity.BillInquiryHistoryEntity;
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
 * 요금조회 이력 Repository 인터페이스
 * 
 * 요금조회 이력 데이터에 대한 접근을 담당하는 Repository
 * - JPA를 통한 기본 CRUD 작업
 * - 복합 조건 검색을 위한 커스텀 쿼리
 * - 페이징 처리를 통한 대용량 데이터 조회
 * - 성능 최적화를 위한 인덱스 활용 쿼리
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Repository
public interface BillInquiryHistoryRepository extends JpaRepository<BillInquiryHistoryEntity, Long> {

    /**
     * 요청 ID로 이력 조회
     * 
     * @param requestId 요청 ID
     * @return 이력 엔티티 (Optional)
     */
    Optional<BillInquiryHistoryEntity> findByRequestId(String requestId);

    /**
     * 회선번호로 이력 목록 조회 (최신순)
     * 
     * @param lineNumber 회선번호
     * @param pageable 페이징 정보
     * @return 이력 페이지
     */
    Page<BillInquiryHistoryEntity> findByLineNumberOrderByRequestTimeDesc(
            String lineNumber, Pageable pageable
    );

    /**
     * 회선번호와 상태로 이력 목록 조회
     * 
     * @param lineNumber 회선번호
     * @param status 처리 상태
     * @param pageable 페이징 정보
     * @return 이력 페이지
     */
    Page<BillInquiryHistoryEntity> findByLineNumberAndStatusOrderByRequestTimeDesc(
            String lineNumber, String status, Pageable pageable
    );

    /**
     * 회선번호 목록으로 이력 조회 (사용자 권한 기반)
     * 
     * @param lineNumbers 회선번호 목록
     * @param pageable 페이징 정보
     * @return 이력 페이지
     */
    Page<BillInquiryHistoryEntity> findByLineNumberInOrderByRequestTimeDesc(
            List<String> lineNumbers, Pageable pageable
    );

    /**
     * 기간별 이력 조회
     * 
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @param pageable 페이징 정보
     * @return 이력 페이지
     */
    Page<BillInquiryHistoryEntity> findByRequestTimeBetweenOrderByRequestTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable
    );

    /**
     * 복합 조건을 통한 이력 조회 (동적 쿼리)
     * 
     * @param lineNumbers 사용자 권한이 있는 회선번호 목록
     * @param lineNumber 특정 회선번호 필터 (선택)
     * @param startTime 조회 시작 시간 (선택)
     * @param endTime 조회 종료 시간 (선택)
     * @param status 처리 상태 필터 (선택)
     * @param pageable 페이징 정보
     * @return 이력 페이지
     */
    @Query("SELECT h FROM BillInquiryHistoryEntity h WHERE " +
           "h.lineNumber IN :lineNumbers " +
           "ORDER BY h.requestTime DESC")
    Page<BillInquiryHistoryEntity> findBillHistoryByLineNumbers(
            @Param("lineNumbers") List<String> lineNumbers,
            Pageable pageable
    );

    @Query("SELECT h FROM BillInquiryHistoryEntity h WHERE " +
           "h.lineNumber IN :lineNumbers " +
           "AND h.lineNumber = :lineNumber " +
           "ORDER BY h.requestTime DESC")
    Page<BillInquiryHistoryEntity> findBillHistoryByLineNumbersAndLineNumber(
            @Param("lineNumbers") List<String> lineNumbers,
            @Param("lineNumber") String lineNumber,
            Pageable pageable
    );

    @Query("SELECT h FROM BillInquiryHistoryEntity h WHERE " +
           "h.lineNumber IN :lineNumbers " +
           "AND h.requestTime >= :startTime " +
           "AND h.requestTime <= :endTime " +
           "ORDER BY h.requestTime DESC")
    Page<BillInquiryHistoryEntity> findBillHistoryByLineNumbersAndDateRange(
            @Param("lineNumbers") List<String> lineNumbers,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    @Query("SELECT h FROM BillInquiryHistoryEntity h WHERE " +
           "h.lineNumber IN :lineNumbers " +
           "AND h.status = :status " +
           "ORDER BY h.requestTime DESC")
    Page<BillInquiryHistoryEntity> findBillHistoryByLineNumbersAndStatus(
            @Param("lineNumbers") List<String> lineNumbers,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT h FROM BillInquiryHistoryEntity h WHERE " +
           "h.lineNumber IN :lineNumbers " +
           "AND h.lineNumber = :lineNumber " +
           "AND h.requestTime >= :startTime " +
           "AND h.requestTime <= :endTime " +
           "ORDER BY h.requestTime DESC")
    Page<BillInquiryHistoryEntity> findBillHistoryByLineNumbersAndLineNumberAndDateRange(
            @Param("lineNumbers") List<String> lineNumbers,
            @Param("lineNumber") String lineNumber,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    @Query("SELECT h FROM BillInquiryHistoryEntity h WHERE " +
           "h.lineNumber IN :lineNumbers " +
           "AND h.lineNumber = :lineNumber " +
           "AND h.status = :status " +
           "ORDER BY h.requestTime DESC")
    Page<BillInquiryHistoryEntity> findBillHistoryByLineNumbersAndLineNumberAndStatus(
            @Param("lineNumbers") List<String> lineNumbers,
            @Param("lineNumber") String lineNumber,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT h FROM BillInquiryHistoryEntity h WHERE " +
           "h.lineNumber IN :lineNumbers " +
           "AND h.requestTime >= :startTime " +
           "AND h.requestTime <= :endTime " +
           "AND h.status = :status " +
           "ORDER BY h.requestTime DESC")
    Page<BillInquiryHistoryEntity> findBillHistoryByLineNumbersAndDateRangeAndStatus(
            @Param("lineNumbers") List<String> lineNumbers,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT h FROM BillInquiryHistoryEntity h WHERE " +
           "h.lineNumber IN :lineNumbers " +
           "AND h.lineNumber = :lineNumber " +
           "AND h.requestTime >= :startTime " +
           "AND h.requestTime <= :endTime " +
           "AND h.status = :status " +
           "ORDER BY h.requestTime DESC")
    Page<BillInquiryHistoryEntity> findBillHistoryWithAllFilters(
            @Param("lineNumbers") List<String> lineNumbers,
            @Param("lineNumber") String lineNumber,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") String status,
            Pageable pageable
    );

    /**
     * 특정 회선의 최근 이력 조회
     * 
     * @param lineNumber 회선번호
     * @param limit 조회 건수
     * @return 최근 이력 목록
     */
    @Query("SELECT h FROM BillInquiryHistoryEntity h WHERE h.lineNumber = :lineNumber " +
           "ORDER BY h.requestTime DESC")
    List<BillInquiryHistoryEntity> findRecentHistoryByLineNumber(
            @Param("lineNumber") String lineNumber, Pageable pageable
    );

    /**
     * 처리 상태별 통계 조회
     * 
     * @param lineNumbers 회선번호 목록
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return 상태별 개수 목록
     */
    @Query("SELECT h.status, COUNT(h) FROM BillInquiryHistoryEntity h WHERE " +
           "h.lineNumber IN :lineNumbers " +
           "AND h.requestTime BETWEEN :startTime AND :endTime " +
           "GROUP BY h.status")
    List<Object[]> getStatusStatistics(
            @Param("lineNumbers") List<String> lineNumbers,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 처리 시간이 긴 요청 조회 (성능 모니터링용)
     * 
     * @param thresholdMs 임계값 (밀리초)
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @param pageable 페이징 정보
     * @return 느린 요청 목록
     */
    @Query("SELECT h FROM BillInquiryHistoryEntity h WHERE " +
           "h.kosResponseTimeMs > :thresholdMs " +
           "AND h.requestTime BETWEEN :startTime AND :endTime " +
           "ORDER BY h.kosResponseTimeMs DESC")
    Page<BillInquiryHistoryEntity> findSlowRequests(
            @Param("thresholdMs") Long thresholdMs,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 캐시 히트율 통계 조회
     * 
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return [총 요청 수, 캐시 히트 수]
     */
    @Query("SELECT COUNT(h), SUM(CASE WHEN h.cacheHit = true THEN 1 ELSE 0 END) " +
           "FROM BillInquiryHistoryEntity h WHERE " +
           "h.requestTime BETWEEN :startTime AND :endTime")
    Object[] getCacheHitRateStatistics(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 실패한 요청 조회 (디버깅용)
     * 
     * @param lineNumbers 회선번호 목록
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @param pageable 페이징 정보
     * @return 실패한 요청 목록
     */
    @Query("SELECT h FROM BillInquiryHistoryEntity h WHERE " +
           "h.lineNumber IN :lineNumbers " +
           "AND h.status = 'FAILED' " +
           "AND h.requestTime BETWEEN :startTime AND :endTime " +
           "ORDER BY h.requestTime DESC")
    Page<BillInquiryHistoryEntity> findFailedRequests(
            @Param("lineNumbers") List<String> lineNumbers,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 오래된 처리 중 상태 요청 조회 (데이터 정리용)
     * 
     * @param thresholdTime 임계 시간 (이 시간 이전의 PROCESSING 상태 요청)
     * @return 오래된 처리 중 요청 목록
     */
    @Query("SELECT h FROM BillInquiryHistoryEntity h WHERE " +
           "h.status = 'PROCESSING' AND h.requestTime < :thresholdTime " +
           "ORDER BY h.requestTime")
    List<BillInquiryHistoryEntity> findOldProcessingRequests(
            @Param("thresholdTime") LocalDateTime thresholdTime
    );

    /**
     * 특정 조회월의 이력 개수 조회
     * 
     * @param lineNumber 회선번호
     * @param inquiryMonth 조회월
     * @return 이력 개수
     */
    long countByLineNumberAndInquiryMonth(String lineNumber, String inquiryMonth);

    /**
     * 회선번호별 이력 개수 조회
     * 
     * @param lineNumbers 회선번호 목록
     * @return 회선번호별 이력 개수
     */
    @Query("SELECT h.lineNumber, COUNT(h) FROM BillInquiryHistoryEntity h WHERE " +
           "h.lineNumber IN :lineNumbers GROUP BY h.lineNumber")
    List<Object[]> getHistoryCountByLineNumber(@Param("lineNumbers") List<String> lineNumbers);

    /**
     * 데이터 정리를 위한 오래된 이력 삭제
     * 
     * @param beforeTime 이 시간 이전의 데이터 삭제
     * @return 삭제된 레코드 수
     */
    @Query("DELETE FROM BillInquiryHistoryEntity h WHERE h.requestTime < :beforeTime")
    int deleteByRequestTimeBefore(@Param("beforeTime") LocalDateTime beforeTime);
}