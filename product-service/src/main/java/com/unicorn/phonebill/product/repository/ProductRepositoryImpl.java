package com.unicorn.phonebill.product.repository;

import com.unicorn.phonebill.product.domain.Product;
import com.unicorn.phonebill.product.domain.ProductStatus;
import com.unicorn.phonebill.product.service.KosClientService;
import com.unicorn.phonebill.product.dto.kos.KosProductInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis 캐시를 활용한 상품 Repository 구현체
 * KOS 시스템 연동 데이터를 캐시로 관리
 */
@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProductRepositoryImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final KosClientService kosClientService;
    
    // 캐시 키 접두사
    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final String PRODUCTS_CACHE_PREFIX = "products:";
    private static final String AVAILABLE_PRODUCTS_KEY = "products:available";
    private static final String CACHE_STATS_KEY = "cache:product:stats";
    
    // 캐시 TTL (초)
    private static final long PRODUCT_CACHE_TTL = 3600; // 1시간
    private static final long PRODUCTS_CACHE_TTL = 1800; // 30분

    public ProductRepositoryImpl(RedisTemplate<String, Object> redisTemplate, 
                                 KosClientService kosClientService) {
        this.redisTemplate = redisTemplate;
        this.kosClientService = kosClientService;
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
            
            // KOS API 호출로 실제 데이터 조회
            List<KosProductInfo> kosProducts = kosClientService.getProductListFromKos();
            List<Product> products = convertKosProductsToProducts(kosProducts);
            
            cacheProducts(products, AVAILABLE_PRODUCTS_KEY);
            logger.info("KOS에서 조회한 상품 개수: {}", products.size());
            return products;
            
        } catch (Exception e) {
            logger.error("Error finding available products from KOS", e);
            // KOS 연동 실패 시 빈 목록 반환 (fallback은 KosClientService에서 처리)
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

    /**
     * KOS 상품 정보를 Product 도메인으로 변환
     */
    private List<Product> convertKosProductsToProducts(List<KosProductInfo> kosProducts) {
        return kosProducts.stream()
                .filter(kosProduct -> "ACTIVE".equalsIgnoreCase(kosProduct.getStatus()))
                .map(this::convertKosProductToProduct)
                .collect(Collectors.toList());
    }

    /**
     * KosProductInfo를 Product로 변환
     */
    private Product convertKosProductToProduct(KosProductInfo kosProduct) {
        return Product.builder()
                .productCode(kosProduct.getProductCode())
                .productName(kosProduct.getProductName())
                .monthlyFee(kosProduct.getMonthlyFee() != null ? 
                    new BigDecimal(kosProduct.getMonthlyFee()) : BigDecimal.ZERO)
                .dataAllowance(formatDataAllowance(kosProduct.getDataAllowance()))
                .voiceAllowance(formatVoiceAllowance(kosProduct.getVoiceAllowance()))
                .smsAllowance(formatSmsAllowance(kosProduct.getSmsAllowance()))
                .status(convertKosStatusToProductStatus(kosProduct.getStatus()))
                .operatorCode(determineOperatorCode(kosProduct))
                .description(kosProduct.getDescription())
                .build();
    }

    /**
     * 데이터 허용량 포맷팅
     */
    private String formatDataAllowance(Integer dataAllowanceGB) {
        if (dataAllowanceGB == null || dataAllowanceGB == 0) {
            return "0GB";
        }
        if (dataAllowanceGB >= 1000) {
            return "무제한";
        }
        return dataAllowanceGB + "GB";
    }

    /**
     * 음성통화 허용량 포맷팅
     */
    private String formatVoiceAllowance(Integer voiceAllowanceMin) {
        if (voiceAllowanceMin == null || voiceAllowanceMin == 0) {
            return "0분";
        }
        if (voiceAllowanceMin >= 10000) {
            return "무제한";
        }
        return voiceAllowanceMin + "분";
    }

    /**
     * SMS 허용량 포맷팅
     */
    private String formatSmsAllowance(Integer smsAllowanceCount) {
        if (smsAllowanceCount == null || smsAllowanceCount == 0) {
            return "0건";
        }
        if (smsAllowanceCount >= 10000) {
            return "무제한";
        }
        return smsAllowanceCount + "건";
    }

    /**
     * KOS 상태를 Product 상태로 변환
     */
    private ProductStatus convertKosStatusToProductStatus(String kosStatus) {
        if ("ACTIVE".equalsIgnoreCase(kosStatus)) {
            return ProductStatus.ACTIVE;
        }
        return ProductStatus.DISCONTINUED;
    }

    /**
     * 사업자 코드 결정 (KOS에서는 별도로 제공하지 않으므로 상품코드나 기타 정보로 추정)
     */
    private String determineOperatorCode(KosProductInfo kosProduct) {
        // 실제 운영에서는 KOS API에서 사업자 정보를 제공하거나
        // 상품코드 패턴으로 판단해야 함
        // 현재는 기본값으로 "KOS"를 사용
        return "KOS";
    }

    // 테스트 데이터 생성 메서드들 (KOS 연동 실패 시 fallback용으로 유지)
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