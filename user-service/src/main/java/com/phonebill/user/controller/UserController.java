package com.phonebill.user.controller;

import com.phonebill.user.dto.UserInfoResponse;
import com.phonebill.user.dto.PermissionsResponse;
import com.phonebill.user.enums.PermissionCode;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "사용자 관리 API")
public class UserController {
    
    private final UserService userService;
    
    /**
     * 모든 사용자 정보 조회
     * @return 사용자 정보 목록
     */
    @Operation(
        summary = "모든 사용자 정보 조회",
        description = "등록된 모든 사용자의 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ResponseEntity<List<UserInfoResponse>> getAllUsers() {
        log.info("모든 사용자 정보 조회 요청");
        
        List<UserInfoResponse> response = userService.getAllUsers();
        
        log.info("모든 사용자 정보 조회 성공: 사용자 수={}", response.size());
        return ResponseEntity.ok(response);
    }
    
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
     * 권한 부여
     * @param userId 사용자 ID
     * @param permissionCode 권한 코드
     * @param grantedBy 권한 부여자
     * @return 처리 결과
     */
    @Operation(
        summary = "권한 부여",
        description = "사용자에게 특정 권한을 부여합니다.\n\n" +
                      "**사용 가능한 권한 코드:**\n" +
                      "- `BILL_INQUIRY`: 요금 조회 서비스 권한\n" +
                      "- `PRODUCT_CHANGE`: 상품 변경 서비스 권한\n" +
                      "- `ADMIN`: 관리자 권한\n" +
                      "- `USER_MANAGEMENT`: 사용자 관리 권한"
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
            @Parameter(
                description = "권한 코드", 
                required = true,
                example = "BILL_INQUIRY"
            )
            @PathVariable String permissionCode,
            @Parameter(description = "권한 부여자", required = true)
            @RequestParam String grantedBy
    ) {
        log.info("권한 부여 요청: userId={}, permissionCode={}, grantedBy={}", 
                userId, permissionCode, grantedBy);
        
        // 권한 코드 유효성 검증
        PermissionCode.fromCode(permissionCode);
        
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
        description = "사용자의 특정 권한을 철회합니다.\n\n" +
                      "**사용 가능한 권한 코드:**\n" +
                      "- `BILL_INQUIRY`: 요금 조회 서비스 권한\n" +
                      "- `PRODUCT_CHANGE`: 상품 변경 서비스 권한\n" +
                      "- `ADMIN`: 관리자 권한\n" +
                      "- `USER_MANAGEMENT`: 사용자 관리 권한"
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
            @Parameter(
                description = "권한 코드", 
                required = true,
                example = "BILL_INQUIRY"
            )
            @PathVariable String permissionCode
    ) {
        log.info("권한 철회 요청: userId={}, permissionCode={}", userId, permissionCode);
        
        // 권한 코드 유효성 검증
        PermissionCode.fromCode(permissionCode);
        
        userService.revokePermission(userId, permissionCode);
        
        log.info("권한 철회 성공: userId={}, permissionCode={}", userId, permissionCode);
        return ResponseEntity.ok("권한이 성공적으로 철회되었습니다.");
    }
    
    
    
    
    
}