package com.phonebill.user.controller;

import com.phonebill.user.dto.*;
import com.phonebill.user.entity.AuthUserEntity;
import com.phonebill.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자 관리 컨트롤러
 * 사용자 정보 조회, 권한 관리 등 사용자 관련 API를 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "사용자 관리 API")
public class UserController {
    
    private final UserService userService;
    
    /**
     * 사용자 정보 조회
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    @Operation(
        summary = "사용자 정보 조회",
        description = "사용자 ID로 사용자의 기본 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserInfoResponse> getUserInfo(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable String userId
    ) {
        log.info("사용자 정보 조회 요청: userId={}", userId);
        
        UserInfoResponse response = userService.getUserInfo(userId);
        
        log.info("사용자 정보 조회 성공: userId={}", userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 고객 ID로 사용자 정보 조회
     * @param customerId 고객 ID
     * @return 사용자 정보
     */
    @Operation(
        summary = "고객 ID로 사용자 정보 조회",
        description = "고객 ID로 해당 고객의 사용자 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<UserInfoResponse> getUserInfoByCustomerId(
            @Parameter(description = "고객 ID", required = true)
            @PathVariable String customerId
    ) {
        log.info("고객 ID로 사용자 정보 조회 요청: customerId={}", customerId);
        
        UserInfoResponse response = userService.getUserInfoByCustomerId(customerId);
        
        log.info("고객 ID로 사용자 정보 조회 성공: customerId={}", customerId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 회선번호로 사용자 정보 조회
     * @param lineNumber 회선번호
     * @return 사용자 정보
     */
    @Operation(
        summary = "회선번호로 사용자 정보 조회",
        description = "회선번호로 해당 회선의 사용자 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/by-line/{lineNumber}")
    public ResponseEntity<UserInfoResponse> getUserInfoByLineNumber(
            @Parameter(description = "회선번호", required = true)
            @PathVariable String lineNumber
    ) {
        log.info("회선번호로 사용자 정보 조회 요청: lineNumber={}", lineNumber);
        
        UserInfoResponse response = userService.getUserInfoByLineNumber(lineNumber);
        
        log.info("회선번호로 사용자 정보 조회 성공: lineNumber={}", lineNumber);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자 권한 목록 조회
     * @param userId 사용자 ID
     * @return 권한 목록
     */
    @Operation(
        summary = "사용자 권한 목록 조회",
        description = "사용자가 보유한 모든 권한 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{userId}/permissions")
    public ResponseEntity<PermissionsResponse> getUserPermissions(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable String userId
    ) {
        log.info("사용자 권한 목록 조회 요청: userId={}", userId);
        
        PermissionsResponse response = userService.getUserPermissions(userId);
        
        log.info("사용자 권한 목록 조회 성공: userId={}, 권한 수={}", userId, response.getPermissions().size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 권한 보유 여부 확인
     * @param request 권한 확인 요청
     * @return 권한 확인 결과
     */
    @Operation(
        summary = "특정 권한 보유 여부 확인",
        description = "사용자가 특정 권한을 보유하고 있는지 확인합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "확인 완료"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/check-permission")
    public ResponseEntity<PermissionCheckResponse> checkPermission(
            @Parameter(description = "권한 확인 요청", required = true)
            @Valid @RequestBody PermissionCheckRequest request
    ) {
        log.info("권한 확인 요청: userId={}, permissionCode={}", 
                request.getUserId(), request.getPermissionCode());
        
        PermissionCheckResponse response = userService.checkPermission(request);
        
        log.info("권한 확인 완료: userId={}, permissionCode={}, hasPermission={}", 
                request.getUserId(), request.getPermissionCode(), response.getHasPermission());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 서비스별 사용자 권한 조회
     * @param userId 사용자 ID
     * @param serviceCode 서비스 코드
     * @return 서비스별 권한 목록
     */
    @Operation(
        summary = "서비스별 사용자 권한 조회",
        description = "특정 서비스에 대한 사용자 권한 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{userId}/permissions/{serviceCode}")
    public ResponseEntity<List<String>> getUserPermissionsByService(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable String userId,
            @Parameter(description = "서비스 코드", required = true)
            @PathVariable String serviceCode
    ) {
        log.info("서비스별 사용자 권한 조회 요청: userId={}, serviceCode={}", userId, serviceCode);
        
        List<String> permissions = userService.getUserPermissionsByService(userId, serviceCode);
        
        log.info("서비스별 사용자 권한 조회 성공: userId={}, serviceCode={}, 권한 수={}", 
                userId, serviceCode, permissions.size());
        return ResponseEntity.ok(permissions);
    }
    
    /**
     * 권한 부여
     * @param userId 사용자 ID
     * @param permissionCode 권한 코드
     * @param grantedBy 권한 부여자
     * @return 처리 결과
     */
    @Operation(
        summary = "권한 부여",
        description = "사용자에게 특정 권한을 부여합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "권한 부여 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "사용자 또는 권한을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/{userId}/permissions/{permissionCode}/grant")
    public ResponseEntity<String> grantPermission(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable String userId,
            @Parameter(description = "권한 코드", required = true)
            @PathVariable String permissionCode,
            @Parameter(description = "권한 부여자", required = true)
            @RequestParam String grantedBy
    ) {
        log.info("권한 부여 요청: userId={}, permissionCode={}, grantedBy={}", 
                userId, permissionCode, grantedBy);
        
        userService.grantPermission(userId, permissionCode, grantedBy);
        
        log.info("권한 부여 성공: userId={}, permissionCode={}", userId, permissionCode);
        return ResponseEntity.ok("권한이 성공적으로 부여되었습니다.");
    }
    
    /**
     * 권한 철회
     * @param userId 사용자 ID
     * @param permissionCode 권한 코드
     * @return 처리 결과
     */
    @Operation(
        summary = "권한 철회",
        description = "사용자의 특정 권한을 철회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "권한 철회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "사용자 또는 권한을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/{userId}/permissions/{permissionCode}")
    public ResponseEntity<String> revokePermission(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable String userId,
            @Parameter(description = "권한 코드", required = true)
            @PathVariable String permissionCode
    ) {
        log.info("권한 철회 요청: userId={}, permissionCode={}", userId, permissionCode);
        
        userService.revokePermission(userId, permissionCode);
        
        log.info("권한 철회 성공: userId={}, permissionCode={}", userId, permissionCode);
        return ResponseEntity.ok("권한이 성공적으로 철회되었습니다.");
    }
    
    /**
     * 계정 상태 조회
     * @param userId 사용자 ID
     * @return 계정 상태
     */
    @Operation(
        summary = "계정 상태 조회",
        description = "사용자 계정의 현재 상태를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{userId}/status")
    public ResponseEntity<AuthUserEntity.AccountStatus> getAccountStatus(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable String userId
    ) {
        log.info("계정 상태 조회 요청: userId={}", userId);
        
        AuthUserEntity.AccountStatus status = userService.getAccountStatus(userId);
        
        log.info("계정 상태 조회 성공: userId={}, status={}", userId, status);
        return ResponseEntity.ok(status);
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
    @PostMapping("/{userId}/unlock")
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
     * 사용자 ID 존재 여부 확인
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    @Operation(
        summary = "사용자 ID 존재 여부 확인",
        description = "해당 사용자 ID가 시스템에 존재하는지 확인합니다."
    )
    @ApiResponse(responseCode = "200", description = "확인 완료")
    @GetMapping("/{userId}/exists")
    public ResponseEntity<Boolean> existsUserId(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable String userId
    ) {
        log.info("사용자 ID 존재 여부 확인 요청: userId={}", userId);
        
        boolean exists = userService.existsUserId(userId);
        
        log.info("사용자 ID 존재 여부 확인 완료: userId={}, exists={}", userId, exists);
        return ResponseEntity.ok(exists);
    }
    
    /**
     * 고객 ID 존재 여부 확인
     * @param customerId 고객 ID
     * @return 존재 여부
     */
    @Operation(
        summary = "고객 ID 존재 여부 확인",
        description = "해당 고객 ID가 시스템에 존재하는지 확인합니다."
    )
    @ApiResponse(responseCode = "200", description = "확인 완료")
    @GetMapping("/customer/{customerId}/exists")
    public ResponseEntity<Boolean> existsCustomerId(
            @Parameter(description = "고객 ID", required = true)
            @PathVariable String customerId
    ) {
        log.info("고객 ID 존재 여부 확인 요청: customerId={}", customerId);
        
        boolean exists = userService.existsCustomerId(customerId);
        
        log.info("고객 ID 존재 여부 확인 완료: customerId={}, exists={}", customerId, exists);
        return ResponseEntity.ok(exists);
    }
    
    /**
     * 헬스 체크
     * @return 서비스 상태
     */
    @Operation(
        summary = "사용자 서비스 헬스 체크",
        description = "사용자 서비스의 상태를 확인합니다."
    )
    @ApiResponse(responseCode = "200", description = "서비스 정상")
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("User Service is running");
    }
}