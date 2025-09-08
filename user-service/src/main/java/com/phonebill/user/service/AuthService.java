package com.phonebill.user.service;

import com.phonebill.user.config.JwtConfig;
import com.phonebill.user.dto.*;
import com.phonebill.user.entity.AuthUserEntity;
import com.phonebill.user.entity.AuthUserSessionEntity;
import com.phonebill.user.exception.*;
import com.phonebill.user.repository.AuthUserRepository;
import com.phonebill.user.repository.AuthUserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * 인증 서비스
 * 로그인, 로그아웃, 토큰 갱신 등 인증 관련 기능을 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    
    private final AuthUserRepository authUserRepository;
    private final AuthUserSessionRepository authUserSessionRepository;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final PasswordEncoder passwordEncoder;
    
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 30 * 60 * 1000L; // 30분
    
    /**
     * 사용자 로그인
     * @param request 로그인 요청 정보
     * @return 로그인 응답 정보
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 사용자 조회
        AuthUserEntity user = authUserRepository.findById(request.getUserId())
                .orElseThrow(() -> UserNotFoundException.byUserId(request.getUserId()));
        
        // 계정 상태 확인
        validateAccountStatus(user);
        
        // 비밀번호 검증
        if (!verifyPassword(request.getPassword(), user.getPasswordHash(), user.getPasswordSalt())) {
            handleLoginFailure(user);
            throw InvalidCredentialsException.invalidPassword();
        }
        
        // 로그인 성공 처리
        handleLoginSuccess(user);
        
        // JWT 토큰 생성
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        // 세션 저장
        saveUserSession(user, refreshToken);
        
        log.info("사용자 로그인 성공: userId={}", user.getUserId());
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn((int) (jwtConfig.getAccessTokenValidity() / 1000)) // 초 단위로 변환
                .userId(user.getUserId())
                .customerId(user.getCustomerId())
                .lineNumber(user.getLineNumber())
                .build();
    }
    
    /**
     * 토큰 갱신
     * @param request 토큰 갱신 요청
     * @return 새로운 토큰 정보
     */
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        // Refresh Token 유효성 검증
        if (!jwtService.validateToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw InvalidTokenException.invalid();
        }
        
        String userId = jwtService.getUserIdFromToken(refreshToken);
        
        // 세션 확인
        Optional<AuthUserSessionEntity> sessionOpt = authUserSessionRepository
                .findByUserIdAndRefreshTokenAndIsActiveTrue(userId, refreshToken);
        
        if (sessionOpt.isEmpty()) {
            throw InvalidTokenException.invalid();
        }
        
        AuthUserSessionEntity session = sessionOpt.get();
        
        // 사용자 조회
        AuthUserEntity user = authUserRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));
        
        // 계정 상태 확인
        validateAccountStatus(user);
        
        // 새로운 토큰 생성
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        
        // 기존 세션 비활성화 및 새 세션 생성
        session.deactivate();
        saveUserSession(user, newRefreshToken);
        
        log.info("토큰 갱신 성공: userId={}", userId);
        
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn((int) (jwtConfig.getAccessTokenValidity() / 1000)) // 초 단위로 변환
                .build();
    }
    
    /**
     * 로그아웃
     * @param userId 사용자 ID
     * @param refreshToken Refresh Token
     */
    @Transactional
    public void logout(String userId, String refreshToken) {
        // 세션 비활성화
        Optional<AuthUserSessionEntity> sessionOpt = authUserSessionRepository
                .findByUserIdAndRefreshTokenAndIsActiveTrue(userId, refreshToken);
        
        sessionOpt.ifPresent(AuthUserSessionEntity::deactivate);
        
        log.info("사용자 로그아웃: userId={}", userId);
    }
    
    /**
     * 토큰 검증
     * @param token 검증할 토큰
     * @return 토큰 검증 결과
     */
    public TokenVerifyResponse verifyToken(String token) {
        try {
            if (!jwtService.validateToken(token)) {
                return TokenVerifyResponse.invalid();
            }
            
            String userId = jwtService.getUserIdFromToken(token);
            String customerId = jwtService.getCustomerIdFromToken(token);
            String lineNumber = jwtService.getLineNumberFromToken(token);
            LocalDateTime expiresAt = jwtService.getExpirationDateFromToken(token);
            
            return TokenVerifyResponse.valid(userId, customerId, lineNumber, expiresAt);
            
        } catch (Exception e) {
            log.warn("토큰 검증 실패: {}", e.getMessage());
            return TokenVerifyResponse.invalid();
        }
    }
    
    /**
     * 계정 상태 검증
     * @param user 사용자 정보
     */
    private void validateAccountStatus(AuthUserEntity user) {
        if (user.isAccountLocked()) {
            throw AccountLockedException.create(user.getUserId(), user.getAccountLockedUntil());
        }
        
        if (!user.isAccountActive()) {
            throw new RuntimeException("비활성 상태인 계정입니다.");
        }
    }
    
    /**
     * 비밀번호 검증
     * @param plainPassword 평문 비밀번호
     * @param hashedPassword 해시된 비밀번호
     * @param salt 솔트
     * @return 검증 결과
     */
    private boolean verifyPassword(String plainPassword, String hashedPassword, String salt) {
        String saltedPassword = plainPassword + salt;
        return passwordEncoder.matches(saltedPassword, hashedPassword);
    }
    
    /**
     * 로그인 실패 처리
     * @param user 사용자 정보
     */
    private void handleLoginFailure(AuthUserEntity user) {
        user.incrementFailedLoginCount();
        
        // 최대 로그인 시도 횟수 초과 시 계정 잠금
        if (user.getFailedLoginCount() >= MAX_LOGIN_ATTEMPTS) {
            user.lockAccount(LOCKOUT_DURATION);
            log.warn("계정 잠금: userId={}, 시도횟수={}", user.getUserId(), user.getFailedLoginCount());
        }
        
        authUserRepository.save(user);
    }
    
    /**
     * 로그인 성공 처리
     * @param user 사용자 정보
     */
    private void handleLoginSuccess(AuthUserEntity user) {
        user.updateLastLogin();
        authUserRepository.save(user);
    }
    
    /**
     * 사용자 세션 저장
     * @param user 사용자 정보
     * @param refreshToken Refresh Token
     */
    private void saveUserSession(AuthUserEntity user, String refreshToken) {
        AuthUserSessionEntity session = AuthUserSessionEntity.builder()
                .userId(user.getUserId())
                .refreshToken(refreshToken)
                .expiresAt(jwtService.getExpirationDateFromToken(refreshToken))
                .isActive(true)
                .build();
        
        authUserSessionRepository.save(session);
    }
    
    /**
     * 솔트 생성
     * @return 랜덤 솔트
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[32];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * 비밀번호 해싱
     * @param plainPassword 평문 비밀번호
     * @param salt 솔트
     * @return 해시된 비밀번호
     */
    private String hashPassword(String plainPassword, String salt) {
        String saltedPassword = plainPassword + salt;
        return passwordEncoder.encode(saltedPassword);
    }
    
    /**
     * 비밀번호 변경
     * @param userId 사용자 ID
     * @param currentPassword 현재 비밀번호
     * @param newPassword 새 비밀번호
     */
    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        AuthUserEntity user = authUserRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));
        
        // 현재 비밀번호 검증
        if (!verifyPassword(currentPassword, user.getPasswordHash(), user.getPasswordSalt())) {
            throw InvalidCredentialsException.invalidPassword();
        }
        
        // 새 비밀번호 해싱
        String newSalt = generateSalt();
        String newHashedPassword = hashPassword(newPassword, newSalt);
        
        // 비밀번호 업데이트
        user.updatePassword(newHashedPassword, newSalt);
        authUserRepository.save(user);
        
        // 모든 세션 무효화
        authUserSessionRepository.deactivateAllUserSessions(userId);
        
        log.info("비밀번호 변경 완료: userId={}", userId);
    }
}