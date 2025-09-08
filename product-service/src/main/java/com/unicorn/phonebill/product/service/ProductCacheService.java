package com.unicorn.phonebill.product.service;

import com.unicorn.phonebill.product.dto.CustomerInfoResponse;
import com.unicorn.phonebill.product.dto.ProductInfoDto;
import com.unicorn.phonebill.product.dto.ProductChangeResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 상품 서비스 캐시 관리 서비스
 * 
 * 주요 기능:
 * - Redis를 활용한 성능 최적화
 * - 데이터 특성에 맞는 TTL 적용
 * - 캐시 무효화 처리
 * - 캐시 키 관리
 */
@Service
public class ProductCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ProductCacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    // 캐시 키 접두사
    private static final String CUSTOMER_PRODUCT_PREFIX = "customerProduct:";
    private static final String CURRENT_PRODUCT_PREFIX = "currentProduct:";
    private static final String AVAILABLE_PRODUCTS_PREFIX = "availableProducts:";
    private static final String PRODUCT_STATUS_PREFIX = "productStatus:";
    private static final String LINE_STATUS_PREFIX = "lineStatus:";
    private static final String MENU_INFO_PREFIX = "menuInfo:";
    private static final String PRODUCT_CHANGE_RESULT_PREFIX = "productChangeResult:";

    public ProductCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ========== 고객상품정보 캐시 (TTL: 4시간) ==========

    /**
     * 고객상품정보 캐시 조회
     */
    @Cacheable(value = "customerProductInfo", key = "#lineNumber", unless = "#result == null")
    public CustomerInfoResponse.CustomerInfo getCustomerProductInfo(String lineNumber) {
        logger.debug("고객상품정보 캐시 조회: {}", lineNumber);
        return null; // 캐시 미스 시 null 반환, 실제 조회는 호출측에서 처리
    }

    /**
     * 고객상품정보 캐시 저장
     */
    public void cacheCustomerProductInfo(String lineNumber, CustomerInfoResponse.CustomerInfo customerInfo) {
        if (StringUtils.hasText(lineNumber) && customerInfo != null) {
            String key = CUSTOMER_PRODUCT_PREFIX + lineNumber;
            redisTemplate.opsForValue().set(key, customerInfo, Duration.ofHours(4));
            logger.debug("고객상품정보 캐시 저장: {}", lineNumber);
        }
    }

    // ========== 현재상품정보 캐시 (TTL: 2시간) ==========

    /**
     * 현재상품정보 캐시 조회
     */
    @Cacheable(value = "currentProductInfo", key = "#productCode", unless = "#result == null")
    public ProductInfoDto getCurrentProductInfo(String productCode) {
        logger.debug("현재상품정보 캐시 조회: {}", productCode);
        return null;
    }

    /**
     * 현재상품정보 캐시 저장
     */
    public void cacheCurrentProductInfo(String productCode, ProductInfoDto productInfo) {
        if (StringUtils.hasText(productCode) && productInfo != null) {
            String key = CURRENT_PRODUCT_PREFIX + productCode;
            redisTemplate.opsForValue().set(key, productInfo, Duration.ofHours(2));
            logger.debug("현재상품정보 캐시 저장: {}", productCode);
        }
    }

    // ========== 가용상품목록 캐시 (TTL: 24시간) ==========

    /**
     * 가용상품목록 캐시 조회
     */
    @Cacheable(value = "availableProducts", key = "#operatorCode ?: 'all'", unless = "#result == null")
    @SuppressWarnings("unchecked")
    public List<ProductInfoDto> getAvailableProducts(String operatorCode) {
        logger.debug("가용상품목록 캐시 조회: {}", operatorCode);
        return null;
    }

    /**
     * 가용상품목록 캐시 저장
     */
    public void cacheAvailableProducts(String operatorCode, List<ProductInfoDto> products) {
        if (products != null) {
            String key = AVAILABLE_PRODUCTS_PREFIX + (operatorCode != null ? operatorCode : "all");
            redisTemplate.opsForValue().set(key, products, Duration.ofHours(24));
            logger.debug("가용상품목록 캐시 저장: {} ({}개)", operatorCode, products.size());
        }
    }

    // ========== 상품상태 캐시 (TTL: 1시간) ==========

    /**
     * 상품상태 캐시 조회
     */
    @Cacheable(value = "productStatus", key = "#productCode", unless = "#result == null")
    public String getProductStatus(String productCode) {
        logger.debug("상품상태 캐시 조회: {}", productCode);
        return null;
    }

    /**
     * 상품상태 캐시 저장
     */
    public void cacheProductStatus(String productCode, String status) {
        if (StringUtils.hasText(productCode) && StringUtils.hasText(status)) {
            String key = PRODUCT_STATUS_PREFIX + productCode;
            redisTemplate.opsForValue().set(key, status, Duration.ofHours(1));
            logger.debug("상품상태 캐시 저장: {} = {}", productCode, status);
        }
    }

    // ========== 회선상태 캐시 (TTL: 30분) ==========

    /**
     * 회선상태 캐시 조회
     */
    @Cacheable(value = "lineStatus", key = "#lineNumber", unless = "#result == null")
    public String getLineStatus(String lineNumber) {
        logger.debug("회선상태 캐시 조회: {}", lineNumber);
        return null;
    }

    /**
     * 회선상태 캐시 저장
     */
    public void cacheLineStatus(String lineNumber, String status) {
        if (StringUtils.hasText(lineNumber) && StringUtils.hasText(status)) {
            String key = LINE_STATUS_PREFIX + lineNumber;
            redisTemplate.opsForValue().set(key, status, Duration.ofMinutes(30));
            logger.debug("회선상태 캐시 저장: {} = {}", lineNumber, status);
        }
    }

    // ========== 메뉴정보 캐시 (TTL: 6시간) ==========

    /**
     * 메뉴정보 캐시 조회
     */
    @Cacheable(value = "menuInfo", key = "#userId", unless = "#result == null")
    public Object getMenuInfo(String userId) {
        logger.debug("메뉴정보 캐시 조회: {}", userId);
        return null;
    }

    /**
     * 메뉴정보 캐시 저장
     */
    public void cacheMenuInfo(String userId, Object menuInfo) {
        if (StringUtils.hasText(userId) && menuInfo != null) {
            String key = MENU_INFO_PREFIX + userId;
            redisTemplate.opsForValue().set(key, menuInfo, Duration.ofHours(6));
            logger.debug("메뉴정보 캐시 저장: {}", userId);
        }
    }

    // ========== 상품변경결과 캐시 (TTL: 1시간) ==========

    /**
     * 상품변경결과 캐시 조회
     */
    @Cacheable(value = "productChangeResult", key = "#requestId", unless = "#result == null")
    public ProductChangeResultResponse.ProductChangeResult getProductChangeResult(String requestId) {
        logger.debug("상품변경결과 캐시 조회: {}", requestId);
        return null;
    }

    /**
     * 상품변경결과 캐시 저장
     */
    public void cacheProductChangeResult(String requestId, ProductChangeResultResponse.ProductChangeResult result) {
        if (StringUtils.hasText(requestId) && result != null) {
            String key = PRODUCT_CHANGE_RESULT_PREFIX + requestId;
            redisTemplate.opsForValue().set(key, result, Duration.ofHours(1));
            logger.debug("상품변경결과 캐시 저장: {}", requestId);
        }
    }

    // ========== 캐시 무효화 ==========

    /**
     * 고객 관련 모든 캐시 무효화
     */
    public void evictCustomerCaches(String lineNumber, String customerId) {
        evictCustomerProductInfo(lineNumber);
        evictLineStatus(lineNumber);
        if (StringUtils.hasText(customerId)) {
            evictMenuInfo(customerId);
        }
        logger.info("고객 관련 캐시 무효화 완료: lineNumber={}, customerId={}", lineNumber, customerId);
    }

    /**
     * 상품 관련 모든 캐시 무효화
     */
    public void evictProductCaches(String productCode, String operatorCode) {
        evictCurrentProductInfo(productCode);
        evictProductStatus(productCode);
        evictAvailableProducts(operatorCode);
        logger.info("상품 관련 캐시 무효화 완료: productCode={}, operatorCode={}", productCode, operatorCode);
    }

    /**
     * 상품변경 완료 후 관련 캐시 무효화
     */
    public void evictProductChangeCaches(String lineNumber, String customerId, String oldProductCode, String newProductCode) {
        // 고객 정보 관련 캐시 무효화
        evictCustomerCaches(lineNumber, customerId);
        
        // 변경 전후 상품 캐시 무효화
        if (StringUtils.hasText(oldProductCode)) {
            evictCurrentProductInfo(oldProductCode);
            evictProductStatus(oldProductCode);
        }
        if (StringUtils.hasText(newProductCode)) {
            evictCurrentProductInfo(newProductCode);
            evictProductStatus(newProductCode);
        }
        
        logger.info("상품변경 관련 캐시 무효화 완료: lineNumber={}, oldProduct={}, newProduct={}", 
                   lineNumber, oldProductCode, newProductCode);
    }

    // ========== 개별 캐시 무효화 메서드들 ==========

    @CacheEvict(value = "customerProductInfo", key = "#lineNumber")
    public void evictCustomerProductInfo(String lineNumber) {
        logger.debug("고객상품정보 캐시 무효화: {}", lineNumber);
    }

    @CacheEvict(value = "currentProductInfo", key = "#productCode")
    public void evictCurrentProductInfo(String productCode) {
        logger.debug("현재상품정보 캐시 무효화: {}", productCode);
    }

    @CacheEvict(value = "availableProducts", key = "#operatorCode ?: 'all'")
    public void evictAvailableProducts(String operatorCode) {
        logger.debug("가용상품목록 캐시 무효화: {}", operatorCode);
    }

    @CacheEvict(value = "productStatus", key = "#productCode")
    public void evictProductStatus(String productCode) {
        logger.debug("상품상태 캐시 무효화: {}", productCode);
    }

    @CacheEvict(value = "lineStatus", key = "#lineNumber")
    public void evictLineStatus(String lineNumber) {
        logger.debug("회선상태 캐시 무효화: {}", lineNumber);
    }

    @CacheEvict(value = "menuInfo", key = "#userId")
    public void evictMenuInfo(String userId) {
        logger.debug("메뉴정보 캐시 무효화: {}", userId);
    }

    @CacheEvict(value = "productChangeResult", key = "#requestId")
    public void evictProductChangeResult(String requestId) {
        logger.debug("상품변경결과 캐시 무효화: {}", requestId);
    }

    // ========== 캐시 통계 및 모니터링 ==========

    /**
     * 캐시 히트율 통계 (모니터링용)
     */
    public void logCacheStatistics() {
        logger.info("Redis 캐시 통계 정보 로깅 (구현 필요)");
        // 실제 구현 시 Redis INFO 명령어 또는 Micrometer 메트릭 활용
    }

    /**
     * 특정 패턴의 캐시 키 개수 조회
     */
    public long getCacheKeyCount(String pattern) {
        try {
            return redisTemplate.keys(pattern).size();
        } catch (Exception e) {
            logger.warn("캐시 키 개수 조회 실패: {}", pattern, e);
            return 0;
        }
    }
}