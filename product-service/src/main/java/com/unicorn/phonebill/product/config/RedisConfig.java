package com.unicorn.phonebill.product.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 설정 클래스
 * 
 * 주요 기능:
 * - Redis 연결 설정
 * - 캐시 매니저 설정
 * - 직렬화/역직렬화 설정
 * - 캐시별 TTL 설정
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate 설정
     * String-Object 형태의 데이터 처리
     * Spring Boot 자동 설정으로 생성된 RedisConnectionFactory를 주입받아 사용
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, 
                                          ObjectMapper.DefaultTyping.NON_FINAL, 
                                          JsonTypeInfo.As.WRAPPER_ARRAY);
        objectMapper.registerModule(new JavaTimeModule());
        
        // JSON 직렬화 설정
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // String 직렬화 설정
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        // Key 직렬화: String
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        
        // Value 직렬화: JSON
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        // 기본 직렬화 설정
        template.setDefaultSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        
        return template;
    }

    /**
     * Spring Cache Manager 설정
     * @Cacheable 어노테이션 사용을 위한 설정
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 기본 캐시 설정
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // 기본 TTL: 1시간
                .disableCachingNullValues()
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(createJsonRedisSerializer()));

        // 캐시별 개별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = createCacheConfigurations();
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * 캐시별 개별 설정
     * 데이터 특성에 맞는 TTL 적용
     */
    private Map<String, RedisCacheConfiguration> createCacheConfigurations() {
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        
        // 고객상품정보: 4시간 (자주 변경되지 않음)
        configMap.put("customerProductInfo", createCacheConfig(Duration.ofHours(4)));
        
        // 현재상품정보: 2시간 (변경 가능성 있음)
        configMap.put("currentProductInfo", createCacheConfig(Duration.ofHours(2)));
        
        // 가용상품목록: 24시간 (상품 정보는 하루 단위로 변경)
        configMap.put("availableProducts", createCacheConfig(Duration.ofHours(24)));
        
        // 상품상태: 1시간 (자주 확인 필요)
        configMap.put("productStatus", createCacheConfig(Duration.ofHours(1)));
        
        // 회선상태: 30분 (실시간 확인 필요)
        configMap.put("lineStatus", createCacheConfig(Duration.ofMinutes(30)));
        
        // 메뉴정보: 6시간 (메뉴는 자주 변경되지 않음)
        configMap.put("menuInfo", createCacheConfig(Duration.ofHours(6)));
        
        // 상품변경결과: 1시간 (결과 조회용)
        configMap.put("productChangeResult", createCacheConfig(Duration.ofHours(1)));
        
        return configMap;
    }

    /**
     * 특정 TTL을 가진 캐시 설정 생성
     */
    private RedisCacheConfiguration createCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(createJsonRedisSerializer()));
    }

    /**
     * JSON 직렬화기 생성
     */
    private Jackson2JsonRedisSerializer<Object> createJsonRedisSerializer() {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                                          ObjectMapper.DefaultTyping.NON_FINAL,
                                          JsonTypeInfo.As.WRAPPER_ARRAY);
        objectMapper.registerModule(new JavaTimeModule());
        
        // setObjectMapper는 deprecated되었으므로 생성자 사용
        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }

    /**
     * Redis 프로퍼티 설정 클래스
     */
    @ConfigurationProperties(prefix = "spring.data.redis")
    public static class RedisProperties {
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int database = 0;
        private Duration timeout = Duration.ofSeconds(2);

        // Getters and Setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public int getDatabase() { return database; }
        public void setDatabase(int database) { this.database = database; }
        
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
    }
}