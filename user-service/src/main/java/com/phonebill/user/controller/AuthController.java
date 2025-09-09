package com.phonebill.user.controller;

import com.phonebill.user.dto.*;
import com.phonebill.user.service.AuthService;
import com.phonebill.user.service.JwtService;
import com.phonebill.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러
 * 로그인, 로그아웃, 토큰 갱신 등 인증 관련 API를 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {
    
    private final AuthService authService;
    private final JwtService jwtService;
    private final UserService userService;
    
    /**
     * 사용자 로그인
     * @param loginRequest 로그인 요청 정보
     * @return 로그인 응답 (JWT 토큰 포함)
     */
    @Operation(
        summary = "사용자 로그인",
        description = "사용자 ID와 비밀번호로 로그인하여 JWT 토큰을 발급받습니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)"),
        @ApiResponse(responseCode = "401", description = "인증 실패 (잘못된 사용자 ID 또는 비밀번호)"),
        @ApiResponse(responseCode = "423", description = "계정 잠금"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Parameter(description = "로그인 요청 정보", required = true)
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        log.info("로그인 요청 받음: userId={}, password존재={}, autoLogin={}", 
                loginRequest.getUserId(), 
                loginRequest.getPassword() != null,
                loginRequest.getAutoLogin());
        
        LoginResponse response = authService.login(loginRequest);
        
        log.info("로그인 성공: userId={}", loginRequest.getUserId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 토큰 갱신
     * @param refreshRequest 토큰 갱신 요청
     * @return 새로운 토큰 정보
     */
    @Operation(
        summary = "토큰 갱신",
        description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Parameter(description = "토큰 갱신 요청", required = true)
            @Valid @RequestBody RefreshTokenRequest refreshRequest
    ) {
        log.info("토큰 갱신 요청");
        
        RefreshTokenResponse response = authService.refreshToken(refreshRequest);
        
        log.info("토큰 갱신 성공");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 로그아웃
     * @param request HTTP 요청 (Authorization Header에서 JWT 토큰 추출)
     * @return 로그아웃 결과
     */
    @Operation(
        summary = "사용자 로그아웃",
        description = "Authorization Header의 JWT 토큰에서 사용자 정보를 추출하여 현재 세션을 종료하고 Refresh Token을 무효화합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        // Authorization Header에서 JWT 토큰 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Authorization Header가 필요합니다.");
        }
        
        String accessToken = authHeader.substring(7); // "Bearer " 제거
        
        // JWT 유효성 확인 (AuthService에서 블랙리스트도 확인함)
        if (!jwtService.validateToken(accessToken)) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰입니다.");
        }
        
        // JWT에서 사용자 ID 추출
        String userId = jwtService.getUserIdFromToken(accessToken);
        if (userId == null) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰입니다.");
        }
        
        log.info("로그아웃 요청: userId={}", userId);
        
        // 해당 사용자의 모든 활성 세션 무효화 (Access Token 기반)
        authService.logoutWithAccessToken(userId, accessToken);
        
        log.info("로그아웃 성공: userId={}", userId);
        return ResponseEntity.ok("로그아웃이 완료되었습니다.");
    }
    
    /**
     * 토큰 검증
     * @param token 검증할 토큰
     * @return 토큰 검증 결과
     */
    @Operation(
        summary = "토큰 검증",
        description = "JWT 토큰의 유효성을 검증하고 토큰 정보를 반환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 검증 완료"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/verify")
    public ResponseEntity<TokenVerifyResponse> verifyToken(
            @Parameter(description = "검증할 JWT 토큰", required = true)
            @RequestParam String token
    ) {
        log.info("토큰 검증 요청");
        
        TokenVerifyResponse response = authService.verifyToken(token);
        
        log.info("토큰 검증 완료: valid={}", response.isValid());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 비밀번호 변경
     * @param userId 사용자 ID
     * @param currentPassword 현재 비밀번호
     * @param newPassword 새 비밀번호
     * @return 변경 결과
     */
    @Operation(
        summary = "비밀번호 변경",
        description = "현재 비밀번호를 확인하고 새로운 비밀번호로 변경합니다. 변경 후 모든 세션이 무효화됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "현재 비밀번호가 올바르지 않음"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Parameter(description = "사용자 ID", required = true)
            @RequestParam String userId,
            @Parameter(description = "현재 비밀번호", required = true)
            @RequestParam String currentPassword,
            @Parameter(description = "새 비밀번호", required = true)
            @RequestParam String newPassword
    ) {
        log.info("비밀번호 변경 요청: userId={}", userId);
        
        authService.changePassword(userId, currentPassword, newPassword);
        
        log.info("비밀번호 변경 성공: userId={}", userId);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다. 다시 로그인해 주세요.");
    }
    
    /**
     * 계정 잠금 해제 (관리자용)
     * @param userId 사용자 ID
     * @return 처리 결과
     */
    @Operation(
        summary = "계정 잠금 해제",
        description = "잠겨있는 사용자 계정의 잠금을 해제합니다. (관리자 권한 필요)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "잠금 해제 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/unlock/{userId}")
    public ResponseEntity<String> unlockAccount(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable String userId
    ) {
        log.info("계정 잠금 해제 요청: userId={}", userId);
        
        userService.unlockAccount(userId);
        
        log.info("계정 잠금 해제 성공: userId={}", userId);
        return ResponseEntity.ok("계정 잠금이 성공적으로 해제되었습니다.");
    }
    
    /**
     * 사용자 등록
     * @param request 사용자 등록 요청
     * @return 등록 결과
     */
    @Operation(
        summary = "사용자 등록",
        description = "새로운 사용자를 등록하고 지정된 권한을 부여합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 등록 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패 또는 중복 데이터)"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> registerUser(
            @Parameter(description = "사용자 등록 요청 정보", required = true)
            @Valid @RequestBody UserRegistrationRequest request
    ) {
        log.info("사용자 등록 요청 받음: request={}", request);
        log.info("상세 정보 - userId={}, customerId={}, lineNumber={}, userName={}", 
                request.getUserId(), request.getCustomerId(), request.getLineNumber(), request.getUserName());
        
        UserRegistrationResponse response = userService.registerUser(request);
        
        log.info("사용자 등록 API 처리 완료: userId={}", request.getUserId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 헬스 체크
     * @return 서비스 상태
     */
    @Operation(
        summary = "인증 서비스 헬스 체크",
        description = "인증 서비스의 상태를 확인합니다."
    )
    @ApiResponse(responseCode = "200", description = "서비스 정상")
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Auth Service is running");
    }
}