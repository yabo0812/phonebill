package com.phonebill.kosmock.repository;

import com.phonebill.kosmock.entity.BillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 요금 정보 Repository
 */
@Repository
public interface BillRepository extends JpaRepository<BillEntity, Long> {
    
    /**
     * 회선번호와 청구월로 요금 정보 조회
     */
    Optional<BillEntity> findByLineNumberAndBillingMonth(String lineNumber, String billingMonth);
    
    /**
     * 회선번호별 요금 이력 조회 (최신순)
     */
    List<BillEntity> findByLineNumberOrderByBillingMonthDesc(String lineNumber);
    
    /**
     * 청구월별 요금 정보 조회
     */
    List<BillEntity> findByBillingMonth(String billingMonth);
    
    /**
     * 회선번호별 특정 개수만큼 최근 요금 이력 조회
     */
    @Query("SELECT b FROM BillEntity b WHERE b.lineNumber = :lineNumber ORDER BY b.billingMonth DESC LIMIT :limit")
    List<BillEntity> findRecentBillsByLineNumber(@Param("lineNumber") String lineNumber, @Param("limit") int limit);
    
    /**
     * 회선번호와 청구월로 요금 정보 존재 여부 확인
     */
    boolean existsByLineNumberAndBillingMonth(String lineNumber, String billingMonth);
    
    /**
     * 회선번호별 요금 정보 개수
     */
    long countByLineNumber(String lineNumber);
}