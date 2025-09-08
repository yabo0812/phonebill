package com.phonebill.bill.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐시 설정
 * 
 * Redis를 활용한 캐싱 시스템 설정
 * - Redis 연결 설정
 * - 직렬화/역직렬화 설정
 * - 캐시별 TTL 설정
 * - Cache Manager 구성
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.redis.timeout:5000}")
    private int redisTimeout;

    /**
     * Redis 연결 팩토리 구성
     * 
     * @return Redis 연결 팩토리
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Redis 연결 설정 - 호스트: {}, 포트: {}, DB: {}", redisHost, redisPort, redisDatabase);

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);

        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            config.setPassword(redisPassword);
        }

        JedisConnectionFactory factory = new JedisConnectionFactory(config);
        factory.setTimeout(redisTimeout);

        log.info("Redis 연결 팩토리 구성 완료");
        return factory;
    }

    /**
     * Redis Template 구성
     * 
     * @param connectionFactory Redis 연결 팩토리
     * @return Redis Template
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.debug("Redis Template 구성 시작");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 직렬화: String 사용
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value 직렬화: JSON 사용
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 기본 직렬화 설정
        template.setDefaultSerializer(jsonSerializer);

        template.afterPropertiesSet();

        log.info("Redis Template 구성 완료");
        return template;
    }

    /**
     * Cache Manager 구성
     * 
     * @param connectionFactory Redis 연결 팩토리
     * @return Cache Manager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.debug("Cache Manager 구성 시작");

        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))  // 기본 TTL: 1시간
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper())))
                .disableCachingNullValues();  // null 값 캐싱 비활성화

        // 캐시별 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 요금 데이터 캐시 (1시간)
        cacheConfigurations.put("billData", defaultConfig.entryTtl(Duration.ofHours(1)));

        // 고객 정보 캐시 (4시간)
        cacheConfigurations.put("customerInfo", defaultConfig.entryTtl(Duration.ofHours(4)));

        // 조회 가능 월 캐시 (24시간)
        cacheConfigurations.put("availableMonths", defaultConfig.entryTtl(Duration.ofHours(24)));

        // 상품 정보 캐시 (2시간)
        cacheConfigurations.put("productInfo", defaultConfig.entryTtl(Duration.ofHours(2)));

        // 회선 상태 캐시 (30분)
        cacheConfigurations.put("lineStatus", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // 시스템 설정 캐시 (12시간)
        cacheConfigurations.put("systemConfig", defaultConfig.entryTtl(Duration.ofHours(12)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()  // 트랜잭션 인식
                .build();

        log.info("Cache Manager 구성 완료 - 캐시 종류: {}개", cacheConfigurations.size());
        return cacheManager;
    }

    /**
     * ObjectMapper 구성
     * 
     * @return JSON 직렬화용 ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Java Time 모듈 등록 (LocalDateTime 등 지원)
        mapper.registerModule(new JavaTimeModule());
        
        // 타입 정보 포함 (다형성 지원)
        mapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance, 
            ObjectMapper.DefaultTyping.NON_FINAL, 
            JsonTypeInfo.As.PROPERTY
        );
        
        log.debug("ObjectMapper 구성 완료");
        return mapper;
    }

    /**
     * Redis 캐시 키 생성기 구성
     * 
     * @return 캐시 키 생성기
     */
    @Bean
    public org.springframework.cache.interceptor.KeyGenerator customKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(target.getClass().getSimpleName()).append(":");
            keyBuilder.append(method.getName()).append(":");
            
            for (Object param : params) {
                if (param != null) {
                    keyBuilder.append(param.toString()).append(":");
                }
            }
            
            // 마지막 콜론 제거
            String key = keyBuilder.toString();
            if (key.endsWith(":")) {
                key = key.substring(0, key.length() - 1);
            }
            
            return key;
        };
    }

    /**
     * Redis 연결 상태 확인
     * 
     * @param redisTemplate Redis Template
     * @return 연결 상태
     */
    @Bean
    public RedisHealthIndicator redisHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        return new RedisHealthIndicator(redisTemplate);
    }

    /**
     * Redis 상태 확인을 위한 헬스 인디케이터
     */
    public static class RedisHealthIndicator {
        private final RedisTemplate<String, Object> redisTemplate;

        public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        /**
         * Redis 연결 상태 확인
         * 
         * @return 연결 가능 여부
         */
        public boolean isRedisAvailable() {
            try {
                String response = redisTemplate.getConnectionFactory().getConnection().ping();
                return "PONG".equals(response);
            } catch (Exception e) {
                log.warn("Redis 연결 상태 확인 실패: {}", e.getMessage());
                return false;
            }
        }

        /**
         * Redis 정보 조회
         * 
         * @return Redis 서버 정보
         */
        public String getRedisInfo() {
            try {
                return redisTemplate.getConnectionFactory().getConnection().info().toString();
            } catch (Exception e) {
                log.warn("Redis 정보 조회 실패: {}", e.getMessage());
                return "정보 조회 실패: " + e.getMessage();
            }
        }
    }
}