package com.phonebill.bill.service;

import com.phonebill.bill.dto.BillInquiryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 요금조회 캐시 서비스
 * 
 * Redis를 활용한 요금 정보 캐싱으로 성능 최적화 구현
 * Cache-Aside 패턴을 적용하여 데이터 일관성과 성능을 균형있게 관리
 * 
 * 캐시 전략:
 * - 요금 정보: 1시간 TTL (외부 시스템 연동 부하 감소)
 * - 고객 정보: 4시간 TTL (변경 빈도가 낮음)
 * - 조회 가능 월: 24시간 TTL (일별 업데이트)
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // 캐시 TTL 상수
    private static final Duration BILL_DATA_TTL = Duration.ofHours(1);
    private static final Duration CUSTOMER_INFO_TTL = Duration.ofHours(4);
    private static final Duration AVAILABLE_MONTHS_TTL = Duration.ofHours(24);

    // 캐시 키 접두사
    private static final String BILL_DATA_PREFIX = "bill:data:";
    private static final String CUSTOMER_INFO_PREFIX = "bill:customer:";
    private static final String AVAILABLE_MONTHS_PREFIX = "bill:months:";

    /**
     * 캐시에서 요금 데이터 조회
     * 
     * 캐시 키: bill:data:{lineNumber}:{inquiryMonth}
     * TTL: 1시간
     * 
     * @param lineNumber 회선번호
     * @param inquiryMonth 조회월
     * @return 캐시된 요금 데이터 (없으면 null)
     */
    @Cacheable(value = "billData", key = "#lineNumber + ':' + #inquiryMonth", unless = "#result == null")
    public BillInquiryResponse getCachedBillData(String lineNumber, String inquiryMonth) {
        log.debug("요금 데이터 캐시 조회 - 회선: {}, 조회월: {}", lineNumber, inquiryMonth);

        String cacheKey = BILL_DATA_PREFIX + lineNumber + ":" + inquiryMonth;
        
        try {
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedData != null) {
                BillInquiryResponse response = objectMapper.convertValue(cachedData, BillInquiryResponse.class);
                log.info("요금 데이터 캐시 히트 - 회선: {}, 조회월: {}", lineNumber, inquiryMonth);
                return response;
            }
            
            log.debug("요금 데이터 캐시 미스 - 회선: {}, 조회월: {}", lineNumber, inquiryMonth);
            return null;
            
        } catch (Exception e) {
            log.error("요금 데이터 캐시 조회 오류 - 회선: {}, 조회월: {}, 오류: {}", 
                    lineNumber, inquiryMonth, e.getMessage());
            return null;
        }
    }

    /**
     * 요금 데이터를 캐시에 저장
     * 
     * @param lineNumber 회선번호
     * @param inquiryMonth 조회월
     * @param billData 요금 데이터
     */
    public void cacheBillData(String lineNumber, String inquiryMonth, BillInquiryResponse billData) {
        log.debug("요금 데이터 캐시 저장 - 회선: {}, 조회월: {}", lineNumber, inquiryMonth);

        // null 값은 캐시하지 않음
        if (billData == null) {
            log.debug("요금 데이터가 null이므로 캐시 저장을 건너뜀 - 회선: {}, 조회월: {}", lineNumber, inquiryMonth);
            return;
        }

        String cacheKey = BILL_DATA_PREFIX + lineNumber + ":" + inquiryMonth;
        
        try {
            redisTemplate.opsForValue().set(cacheKey, billData, BILL_DATA_TTL);
            log.info("요금 데이터 캐시 저장 완료 - 회선: {}, 조회월: {}, TTL: {}시간", 
                    lineNumber, inquiryMonth, BILL_DATA_TTL.toHours());
        } catch (Exception e) {
            log.error("요금 데이터 캐시 저장 오류 - 회선: {}, 조회월: {}, 오류: {}", 
                    lineNumber, inquiryMonth, e.getMessage());
        }
    }

    /**
     * 고객 정보 캐시 조회
     * 
     * 캐시 키: bill:customer:{lineNumber}
     * TTL: 4시간
     * 
     * @param lineNumber 회선번호
     * @return 캐시된 고객 정보 (없으면 null)
     */
    public Object getCachedCustomerInfo(String lineNumber) {
        log.debug("고객 정보 캐시 조회 - 회선: {}", lineNumber);

        String cacheKey = CUSTOMER_INFO_PREFIX + lineNumber;
        
        try {
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedData != null) {
                log.info("고객 정보 캐시 히트 - 회선: {}", lineNumber);
                return cachedData;
            }
            
            log.debug("고객 정보 캐시 미스 - 회선: {}", lineNumber);
            return null;
            
        } catch (Exception e) {
            log.error("고객 정보 캐시 조회 오류 - 회선: {}, 오류: {}", lineNumber, e.getMessage());
            return null;
        }
    }

    /**
     * 고객 정보를 캐시에 저장
     * 
     * @param lineNumber 회선번호
     * @param customerInfo 고객 정보
     */
    public void cacheCustomerInfo(String lineNumber, Object customerInfo) {
        log.debug("고객 정보 캐시 저장 - 회선: {}", lineNumber);

        String cacheKey = CUSTOMER_INFO_PREFIX + lineNumber;
        
        try {
            redisTemplate.opsForValue().set(cacheKey, customerInfo, CUSTOMER_INFO_TTL);
            log.info("고객 정보 캐시 저장 완료 - 회선: {}, TTL: {}시간", 
                    lineNumber, CUSTOMER_INFO_TTL.toHours());
        } catch (Exception e) {
            log.error("고객 정보 캐시 저장 오류 - 회선: {}, 오류: {}", lineNumber, e.getMessage());
        }
    }

    /**
     * 특정 회선의 요금 데이터 캐시 무효화
     * 
     * 상품 변경 등으로 요금 정보가 변경된 경우 호출
     * 
     * @param lineNumber 회선번호
     */
    @CacheEvict(value = "billData", key = "#lineNumber + '*'")
    public void evictBillDataCache(String lineNumber) {
        log.info("요금 데이터 캐시 무효화 - 회선: {}", lineNumber);

        try {
            // 패턴을 사용한 키 삭제
            String pattern = BILL_DATA_PREFIX + lineNumber + ":*";
            redisTemplate.delete(redisTemplate.keys(pattern));
            
            log.info("요금 데이터 캐시 무효화 완료 - 회선: {}", lineNumber);
        } catch (Exception e) {
            log.error("요금 데이터 캐시 무효화 오류 - 회선: {}, 오류: {}", lineNumber, e.getMessage());
        }
    }

    /**
     * 특정 월의 모든 요금 데이터 캐시 무효화
     * 
     * 시스템 점검이나 대량 데이터 업데이트 시 사용
     * 
     * @param inquiryMonth 조회월
     */
    public void evictBillDataCacheByMonth(String inquiryMonth) {
        log.info("월별 요금 데이터 캐시 무효화 - 조회월: {}", inquiryMonth);

        try {
            // 패턴을 사용한 키 삭제
            String pattern = BILL_DATA_PREFIX + "*:" + inquiryMonth;
            redisTemplate.delete(redisTemplate.keys(pattern));
            
            log.info("월별 요금 데이터 캐시 무효화 완료 - 조회월: {}", inquiryMonth);
        } catch (Exception e) {
            log.error("월별 요금 데이터 캐시 무효화 오류 - 조회월: {}, 오류: {}", inquiryMonth, e.getMessage());
        }
    }

    /**
     * 전체 요금 데이터 캐시 무효화
     * 
     * 시스템 점검이나 긴급 상황에서 사용
     */
    @CacheEvict(value = "billData", allEntries = true)
    public void evictAllBillDataCache() {
        log.warn("전체 요금 데이터 캐시 무효화 실행");

        try {
            // 모든 요금 데이터 캐시 삭제
            String pattern = BILL_DATA_PREFIX + "*";
            redisTemplate.delete(redisTemplate.keys(pattern));
            
            log.warn("전체 요금 데이터 캐시 무효화 완료");
        } catch (Exception e) {
            log.error("전체 요금 데이터 캐시 무효화 오류: {}", e.getMessage());
        }
    }

    /**
     * 캐시 상태 확인
     * 
     * @param lineNumber 회선번호
     * @param inquiryMonth 조회월
     * @return 캐시 존재 여부
     */
    public boolean isCacheExists(String lineNumber, String inquiryMonth) {
        String cacheKey = BILL_DATA_PREFIX + lineNumber + ":" + inquiryMonth;
        return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
    }

    /**
     * 캐시 만료 시간 조회
     * 
     * @param lineNumber 회선번호  
     * @param inquiryMonth 조회월
     * @return 캐시 만료까지 남은 시간 (초)
     */
    public Long getCacheExpiry(String lineNumber, String inquiryMonth) {
        String cacheKey = BILL_DATA_PREFIX + lineNumber + ":" + inquiryMonth;
        return redisTemplate.getExpire(cacheKey);
    }
}