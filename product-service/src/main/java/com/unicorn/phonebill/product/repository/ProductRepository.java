package com.unicorn.phonebill.product.repository;

import com.unicorn.phonebill.product.domain.Product;
import com.unicorn.phonebill.product.domain.ProductStatus;

import java.util.List;
import java.util.Optional;

/**
 * 상품 Repository 인터페이스
 * Redis 캐시를 통한 KOS 연동 데이터 관리
 */
public interface ProductRepository {

    /**
     * 상품 코드로 상품 조회
     */
    Optional<Product> findByProductCode(String productCode);

    /**
     * 판매 중인 상품 목록 조회
     */
    List<Product> findAvailableProducts();

    /**
     * 사업자별 판매 중인 상품 목록 조회
     */
    List<Product> findAvailableProductsByOperator(String operatorCode);

    /**
     * 상품 상태별 조회
     */
    List<Product> findByStatus(ProductStatus status);

    /**
     * 상품 정보 캐시에 저장
     */
    void cacheProduct(Product product);

    /**
     * 상품 목록 캐시에 저장
     */
    void cacheProducts(List<Product> products, String cacheKey);

    /**
     * 상품 캐시 무효화
     */
    void evictProductCache(String productCode);

    /**
     * 전체 상품 캐시 무효화
     */
    void evictAllProductsCache();

    /**
     * 캐시 적중률 확인
     */
    double getProductCacheHitRate();
}