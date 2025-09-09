package com.phonebill.kosmock.repository;

import com.phonebill.kosmock.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 고객 정보 Repository
 */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, String> {
    
    /**
     * 고객 ID로 조회
     */
    Optional<CustomerEntity> findByCustomerId(String customerId);
    
    /**
     * 회선 번호로 조회
     */
    Optional<CustomerEntity> findByLineNumber(String lineNumber);
    
    /**
     * 상품 코드별 고객 목록 조회
     */
    List<CustomerEntity> findByCurrentProductCode(String productCode);
    
    /**
     * 회선 상태별 고객 목록 조회
     */
    List<CustomerEntity> findByLineStatus(String lineStatus);
    
    /**
     * 고객 ID와 회선번호로 존재 여부 확인
     */
    boolean existsByCustomerIdAndLineNumber(String customerId, String lineNumber);
    
    /**
     * 고객 ID와 회선번호로 조회
     */
    Optional<CustomerEntity> findByCustomerIdAndLineNumber(String customerId, String lineNumber);
    
    /**
     * 상품 정보와 함께 고객 조회
     */
    @Query("SELECT c FROM CustomerEntity c LEFT JOIN FETCH c.product WHERE c.lineNumber = :lineNumber")
    Optional<CustomerEntity> findByLineNumberWithProduct(@Param("lineNumber") String lineNumber);
}