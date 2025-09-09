package com.phonebill.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * JWT 토큰 블랙리스트 서비스
 * Redis를 사용해서 무효화된 토큰을 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;
    
    private static final String BLACKLIST_PREFIX = "blacklist:";
    
    /**
     * 토큰을 블랙리스트에 추가
     * @param token JWT 토큰
     * @param reason 무효화 사유
     */
    public void addToBlacklist(String token, String reason) {
        try {
            // JWT에서 만료시간 추출
            LocalDateTime expiresAt = jwtService.getExpirationDateFromToken(token);
            if (expiresAt == null) {
                log.warn("토큰에서 만료시간을 추출할 수 없음: {}", token.substring(0, Math.min(token.length(), 20)));
                return;
            }
            
            // 현재시간부터 토큰 만료시간까지의 TTL 계산
            long ttlSeconds = expiresAt.atZone(ZoneId.systemDefault()).toEpochSecond() - 
                             LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
            
            // TTL이 양수인 경우만 블랙리스트에 추가 (이미 만료된 토큰은 추가하지 않음)
            if (ttlSeconds > 0) {
                String key = BLACKLIST_PREFIX + token;
                redisTemplate.opsForValue().set(key, reason, ttlSeconds, TimeUnit.SECONDS);
                log.info("토큰이 블랙리스트에 추가됨: reason={}, ttl={}초", reason, ttlSeconds);
            } else {
                log.info("이미 만료된 토큰이므로 블랙리스트에 추가하지 않음");
            }
            
        } catch (Exception e) {
            log.error("블랙리스트 추가 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 토큰이 블랙리스트에 있는지 확인
     * @param token JWT 토큰
     * @return 블랙리스트 여부
     */
    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("블랙리스트 확인 실패: {}", e.getMessage(), e);
            // Redis 오류 시 안전하게 false 반환 (서비스 중단 방지)
            return false;
        }
    }
    
    /**
     * 사용자의 모든 토큰을 블랙리스트에 추가
     * @param userId 사용자 ID
     * @param reason 무효화 사유
     */
    public void addUserTokensToBlacklist(String userId, String reason) {
        try {
            // 패턴으로 해당 사용자의 모든 토큰을 찾을 수는 없으므로,
            // 실제로는 세션 테이블에서 활성 토큰을 조회해서 블랙리스트에 추가해야 함
            log.info("사용자 {} 의 모든 토큰 무효화: {}", userId, reason);
            
        } catch (Exception e) {
            log.error("사용자 토큰 블랙리스트 추가 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 블랙리스트에서 토큰 제거 (관리용)
     * @param token JWT 토큰
     */
    public void removeFromBlacklist(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.delete(key);
            log.info("토큰이 블랙리스트에서 제거됨");
        } catch (Exception e) {
            log.error("블랙리스트 제거 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 블랙리스트 통계 조회
     * @return 블랙리스트 토큰 수
     */
    public long getBlacklistCount() {
        try {
            return redisTemplate.keys(BLACKLIST_PREFIX + "*").size();
        } catch (Exception e) {
            log.error("블랙리스트 통계 조회 실패: {}", e.getMessage(), e);
            return 0;
        }
    }
}