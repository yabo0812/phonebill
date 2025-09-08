package com.unicorn.phonebill.product.repository;

import com.unicorn.phonebill.product.domain.Product;
import com.unicorn.phonebill.product.domain.ProductStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis 캐시를 활용한 상품 Repository 구현체
 * KOS 시스템 연동 데이터를 캐시로 관리
 */
@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProductRepositoryImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    
    // 캐시 키 접두사
    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final String PRODUCTS_CACHE_PREFIX = "products:";
    private static final String AVAILABLE_PRODUCTS_KEY = "products:available";
    private static final String CACHE_STATS_KEY = "cache:product:stats";
    
    // 캐시 TTL (초)
    private static final long PRODUCT_CACHE_TTL = 3600; // 1시간
    private static final long PRODUCTS_CACHE_TTL = 1800; // 30분

    public ProductRepositoryImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<Product> findByProductCode(String productCode) {
        try {
            String cacheKey = PRODUCT_CACHE_PREFIX + productCode;
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached instanceof Product) {
                logger.debug("Cache hit for product: {}", productCode);
                incrementCacheHits();
                return Optional.of((Product) cached);
            }
            
            logger.debug("Cache miss for product: {}", productCode);
            incrementCacheMisses();
            
            // TODO: KOS API 호출로 실제 데이터 조회
            // 현재는 테스트 데이터 반환
            return createTestProduct(productCode);
            
        } catch (Exception e) {
            logger.error("Error finding product by code: {}", productCode, e);
            return Optional.empty();
        }
    }

    @Override
    public List<Product> findAvailableProducts() {
        try {
            @SuppressWarnings("unchecked")
            List<Product> cached = (List<Product>) redisTemplate.opsForValue().get(AVAILABLE_PRODUCTS_KEY);
            
            if (cached != null) {
                logger.debug("Cache hit for available products");
                incrementCacheHits();
                return cached;
            }
            
            logger.debug("Cache miss for available products");
            incrementCacheMisses();
            
            // TODO: KOS API 호출로 실제 데이터 조회
            // 현재는 테스트 데이터 반환
            List<Product> products = createTestAvailableProducts();
            cacheProducts(products, AVAILABLE_PRODUCTS_KEY);
            return products;
            
        } catch (Exception e) {
            logger.error("Error finding available products", e);
            return List.of();
        }
    }

    @Override
    public List<Product> findAvailableProductsByOperator(String operatorCode) {
        try {
            String cacheKey = PRODUCTS_CACHE_PREFIX + "operator:" + operatorCode;
            @SuppressWarnings("unchecked")
            List<Product> cached = (List<Product>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                logger.debug("Cache hit for operator products: {}", operatorCode);
                incrementCacheHits();
                return cached;
            }
            
            logger.debug("Cache miss for operator products: {}", operatorCode);
            incrementCacheMisses();
            
            // TODO: KOS API 호출로 실제 데이터 조회
            // 현재는 테스트 데이터 반환
            List<Product> products = createTestProductsByOperator(operatorCode);
            cacheProducts(products, cacheKey);
            return products;
            
        } catch (Exception e) {
            logger.error("Error finding products by operator: {}", operatorCode, e);
            return List.of();
        }
    }

    @Override
    public List<Product> findByStatus(ProductStatus status) {
        try {
            String cacheKey = PRODUCTS_CACHE_PREFIX + "status:" + status;
            @SuppressWarnings("unchecked")
            List<Product> cached = (List<Product>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                logger.debug("Cache hit for products by status: {}", status);
                incrementCacheHits();
                return cached;
            }
            
            logger.debug("Cache miss for products by status: {}", status);
            incrementCacheMisses();
            
            // TODO: KOS API 호출로 실제 데이터 조회
            // 현재는 테스트 데이터 반환
            List<Product> products = createTestProductsByStatus(status);
            cacheProducts(products, cacheKey);
            return products;
            
        } catch (Exception e) {
            logger.error("Error finding products by status: {}", status, e);
            return List.of();
        }
    }

    @Override
    public void cacheProduct(Product product) {
        try {
            String cacheKey = PRODUCT_CACHE_PREFIX + product.getProductCode();
            redisTemplate.opsForValue().set(cacheKey, product, PRODUCT_CACHE_TTL, TimeUnit.SECONDS);
            logger.debug("Cached product: {}", product.getProductCode());
        } catch (Exception e) {
            logger.error("Error caching product: {}", product.getProductCode(), e);
        }
    }

    @Override
    public void cacheProducts(List<Product> products, String cacheKey) {
        try {
            redisTemplate.opsForValue().set(cacheKey, products, PRODUCTS_CACHE_TTL, TimeUnit.SECONDS);
            logger.debug("Cached products list with key: {}", cacheKey);
        } catch (Exception e) {
            logger.error("Error caching products list: {}", cacheKey, e);
        }
    }

    @Override
    public void evictProductCache(String productCode) {
        try {
            String cacheKey = PRODUCT_CACHE_PREFIX + productCode;
            redisTemplate.delete(cacheKey);
            logger.debug("Evicted product cache: {}", productCode);
        } catch (Exception e) {
            logger.error("Error evicting product cache: {}", productCode, e);
        }
    }

    @Override
    public void evictAllProductsCache() {
        try {
            redisTemplate.delete(redisTemplate.keys(PRODUCT_CACHE_PREFIX + "*"));
            redisTemplate.delete(redisTemplate.keys(PRODUCTS_CACHE_PREFIX + "*"));
            logger.info("Evicted all product caches");
        } catch (Exception e) {
            logger.error("Error evicting all product caches", e);
        }
    }

    @Override
    public double getProductCacheHitRate() {
        try {
            Long hits = (Long) redisTemplate.opsForHash().get(CACHE_STATS_KEY, "hits");
            Long misses = (Long) redisTemplate.opsForHash().get(CACHE_STATS_KEY, "misses");
            
            if (hits == null) hits = 0L;
            if (misses == null) misses = 0L;
            
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        } catch (Exception e) {
            logger.error("Error getting cache hit rate", e);
            return 0.0;
        }
    }

    private void incrementCacheHits() {
        try {
            redisTemplate.opsForHash().increment(CACHE_STATS_KEY, "hits", 1);
        } catch (Exception e) {
            logger.debug("Error incrementing cache hits", e);
        }
    }

    private void incrementCacheMisses() {
        try {
            redisTemplate.opsForHash().increment(CACHE_STATS_KEY, "misses", 1);
        } catch (Exception e) {
            logger.debug("Error incrementing cache misses", e);
        }
    }

    // 테스트 데이터 생성 메서드들 (실제 운영에서는 KOS API 호출로 대체)
    private Optional<Product> createTestProduct(String productCode) {
        Product product = Product.builder()
                .productCode(productCode)
                .productName("테스트 상품 " + productCode)
                .monthlyFee(new java.math.BigDecimal("50000"))
                .dataAllowance("50GB")
                .voiceAllowance("무제한")
                .smsAllowance("무제한")
                .status(ProductStatus.ACTIVE)
                .operatorCode("SKT")
                .description("테스트용 상품입니다.")
                .build();
        
        cacheProduct(product);
        return Optional.of(product);
    }

    private List<Product> createTestAvailableProducts() {
        return List.of(
            createTestProductInstance("LTE_50G", "LTE 50GB 요금제", "50000", "50GB"),
            createTestProductInstance("LTE_100G", "LTE 100GB 요금제", "70000", "100GB"),
            createTestProductInstance("5G_100G", "5G 100GB 요금제", "80000", "100GB")
        );
    }

    private List<Product> createTestProductsByOperator(String operatorCode) {
        return List.of(
            createTestProductInstance("LTE_30G_" + operatorCode, operatorCode + " LTE 30GB", "45000", "30GB"),
            createTestProductInstance("5G_50G_" + operatorCode, operatorCode + " 5G 50GB", "65000", "50GB")
        );
    }

    private List<Product> createTestProductsByStatus(ProductStatus status) {
        if (status == ProductStatus.ACTIVE) {
            return createTestAvailableProducts();
        }
        return List.of();
    }

    private Product createTestProductInstance(String code, String name, String fee, String dataAllowance) {
        return Product.builder()
                .productCode(code)
                .productName(name)
                .monthlyFee(new java.math.BigDecimal(fee))
                .dataAllowance(dataAllowance)
                .voiceAllowance("무제한")
                .smsAllowance("무제한")
                .status(ProductStatus.ACTIVE)
                .operatorCode("SKT")
                .description("테스트용 상품")
                .build();
    }
}