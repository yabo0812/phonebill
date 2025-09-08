package com.phonebill.kosmock.controller;

import com.phonebill.kosmock.dto.*;
import com.phonebill.kosmock.service.KosMockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * KOS Mock API 컨트롤러
 * KT 통신사 시스템(KOS-Order)의 API를 모방합니다.
 */
@RestController
@RequestMapping("/api/v1/kos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "KOS Mock API", description = "KT 통신사 시스템 Mock API")
public class KosMockController {

    private final KosMockService kosMockService;

    /**
     * 요금 조회 API
     */
    @PostMapping("/bill/inquiry")
    @Operation(summary = "요금 조회", description = "고객의 통신요금 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", 
                    content = @Content(schema = @Schema(implementation = KosCommonResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<KosCommonResponse<KosBillInquiryResponse>> inquireBill(
            @Valid @RequestBody KosBillInquiryRequest request) {
        
        log.info("요금 조회 요청 수신 - RequestId: {}, LineNumber: {}", 
                request.getRequestId(), request.getLineNumber());
        
        try {
            KosBillInquiryResponse response = kosMockService.processBillInquiry(request);
            
            if ("0000".equals(response.getResultCode())) {
                return ResponseEntity.ok(KosCommonResponse.success(response, "요금 조회가 완료되었습니다"));
            } else {
                return ResponseEntity.ok(KosCommonResponse.failure(
                        response.getResultCode(), response.getResultMessage()));
            }
            
        } catch (Exception e) {
            log.error("요금 조회 처리 중 오류 발생 - RequestId: {}", request.getRequestId(), e);
            return ResponseEntity.ok(KosCommonResponse.systemError());
        }
    }

    /**
     * 상품 변경 API
     */
    @PostMapping("/product/change")
    @Operation(summary = "상품 변경", description = "고객의 통신상품을 변경합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "변경 처리 성공", 
                    content = @Content(schema = @Schema(implementation = KosCommonResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<KosCommonResponse<KosProductChangeResponse>> changeProduct(
            @Valid @RequestBody KosProductChangeRequest request) {
        
        log.info("상품 변경 요청 수신 - RequestId: {}, LineNumber: {}, Target: {}", 
                request.getRequestId(), request.getLineNumber(), request.getTargetProductCode());
        
        try {
            KosProductChangeResponse response = kosMockService.processProductChange(request);
            
            if ("0000".equals(response.getResultCode())) {
                return ResponseEntity.ok(KosCommonResponse.success(response, "상품 변경이 완료되었습니다"));
            } else {
                return ResponseEntity.ok(KosCommonResponse.failure(
                        response.getResultCode(), response.getResultMessage()));
            }
            
        } catch (Exception e) {
            log.error("상품 변경 처리 중 오류 발생 - RequestId: {}", request.getRequestId(), e);
            return ResponseEntity.ok(KosCommonResponse.systemError());
        }
    }

    /**
     * 처리 상태 조회 API
     */
    @GetMapping("/status/{requestId}")
    @Operation(summary = "처리 상태 조회", description = "요청의 처리 상태를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "요청 ID를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<KosCommonResponse<Object>> getProcessingStatus(
            @Parameter(description = "요청 ID", example = "REQ_20250108_001")
            @PathVariable String requestId) {
        
        log.info("처리 상태 조회 요청 - RequestId: {}", requestId);
        
        try {
            // Mock 데이터에서 처리 결과 조회 로직은 간단하게 구현
            // 실제로는 mockDataService.getProcessingResult(requestId) 사용
            
            return ResponseEntity.ok(KosCommonResponse.success(
                "PROCESSING 상태 - 처리 중입니다.", 
                "처리 상태 조회가 완료되었습니다"));
                
        } catch (Exception e) {
            log.error("처리 상태 조회 중 오류 발생 - RequestId: {}", requestId, e);
            return ResponseEntity.ok(KosCommonResponse.systemError());
        }
    }

    /**
     * 서비스 상태 체크 API
     */
    @GetMapping("/health")
    @Operation(summary = "서비스 상태 체크", description = "KOS Mock 서비스의 상태를 확인합니다.")
    public ResponseEntity<KosCommonResponse<Object>> healthCheck() {
        
        log.debug("KOS Mock 서비스 상태 체크 요청");
        
        try {
            return ResponseEntity.ok(KosCommonResponse.success(
                "KOS Mock Service is running normally", 
                "서비스가 정상 동작 중입니다"));
                
        } catch (Exception e) {
            log.error("서비스 상태 체크 중 오류 발생", e);
            return ResponseEntity.ok(KosCommonResponse.systemError());
        }
    }

    /**
     * Mock 설정 조회 API (개발/테스트용)
     */
    @GetMapping("/mock/config")
    @Operation(summary = "Mock 설정 조회", description = "현재 Mock 서비스의 설정을 조회합니다. (개발/테스트용)")
    public ResponseEntity<KosCommonResponse<Object>> getMockConfig() {
        
        log.info("Mock 설정 조회 요청");
        
        try {
            // Mock 설정 정보를 간단히 반환
            String configInfo = String.format(
                "Response Delay: %dms, Failure Rate: %.2f%%, Service Status: ACTIVE",
                500, 1.0); // 하드코딩된 값 (실제로는 MockConfig에서 가져올 수 있음)
                
            return ResponseEntity.ok(KosCommonResponse.success(
                configInfo, 
                "Mock 설정 조회가 완료되었습니다"));
                
        } catch (Exception e) {
            log.error("Mock 설정 조회 중 오류 발생", e);
            return ResponseEntity.ok(KosCommonResponse.systemError());
        }
    }
}