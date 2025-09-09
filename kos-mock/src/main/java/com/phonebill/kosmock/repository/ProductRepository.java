package com.phonebill.kosmock.repository;

import com.phonebill.kosmock.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 상품 정보 Repository
 */
@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, String> {
    
    /**
     * 활성화된 상품 목록 조회
     */
    List<ProductEntity> findByStatusOrderByMonthlyFeeDesc(String status);
    
    /**
     * 네트워크 타입별 상품 조회
     */
    List<ProductEntity> findByNetworkTypeAndStatusOrderByMonthlyFeeDesc(String networkType, String status);
    
    /**
     * 상품 코드로 조회
     */
    Optional<ProductEntity> findByProductCode(String productCode);
    
    /**
     * 상품 존재 여부 확인
     */
    boolean existsByProductCode(String productCode);
    
    /**
     * 전체 상품 수 조회
     */
    @Query("SELECT COUNT(p) FROM ProductEntity p")
    long countAllProducts();
}