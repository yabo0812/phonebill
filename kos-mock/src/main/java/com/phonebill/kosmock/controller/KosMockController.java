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
     * 상품 정보 목록 조회 API
     */
    @GetMapping("/product/list")
    @Operation(summary = "상품 목록 조회", description = "등록된 통신 상품들의 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", 
                    content = @Content(schema = @Schema(implementation = KosCommonResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<KosCommonResponse<KosProductListResponse>> getProductList() {
        
        log.info("상품 목록 조회 요청 수신");
        
        try {
            KosProductListResponse response = kosMockService.getProductList();
            
            if ("0000".equals(response.getResultCode())) {
                return ResponseEntity.ok(KosCommonResponse.success(response, "상품 목록 조회가 완료되었습니다"));
            } else {
                return ResponseEntity.ok(KosCommonResponse.failure(
                        response.getResultCode(), response.getResultMessage()));
            }
            
        } catch (Exception e) {
            log.error("상품 목록 조회 처리 중 오류 발생", e);
            return ResponseEntity.ok(KosCommonResponse.systemError());
        }
    }

    /**
     * 데이터 보유 월 목록 조회 API
     */
    @GetMapping("/bill/available-months/{lineNumber}")
    @Operation(summary = "데이터 보유 월 목록 조회", description = "회선번호의 실제 요금 데이터가 있는 월 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", 
                    content = @Content(schema = @Schema(implementation = KosCommonResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<KosCommonResponse<KosAvailableMonthsResponse>> getAvailableMonths(
            @Parameter(description = "회선번호 (하이픈 제거된 형태)", example = "01012345678")
            @PathVariable String lineNumber) {
        
        log.info("데이터 보유 월 목록 조회 요청 수신 - LineNumber: {}", lineNumber);
        
        try {
            // 하이픈 없는 형태 그대로 사용 (MockDataService와 일치)
            KosAvailableMonthsResponse response = kosMockService.getAvailableMonths(lineNumber);
            
            if ("0000".equals(response.getResultCode())) {
                return ResponseEntity.ok(KosCommonResponse.success(response, "데이터 보유 월 목록 조회가 완료되었습니다"));
            } else {
                return ResponseEntity.ok(KosCommonResponse.failure(
                        response.getResultCode(), response.getResultMessage()));
            }
            
        } catch (Exception e) {
            log.error("데이터 보유 월 목록 조회 처리 중 오류 발생 - LineNumber: {}", lineNumber, e);
            return ResponseEntity.ok(KosCommonResponse.systemError());
        }
    }
    
    /**
     * 가입상품 조회 API
     */
    @PostMapping("/product/inquiry")
    @Operation(summary = "가입상품 조회", description = "고객의 가입상품 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", 
                    content = @Content(schema = @Schema(implementation = KosCommonResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<KosCommonResponse<KosProductInquiryResponse>> inquireProduct(
            @Valid @RequestBody KosProductInquiryRequest request) {
        
        log.info("가입상품 조회 요청 수신 - RequestId: {}, LineNumber: {}", 
                request.getRequestId(), request.getLineNumber());
        
        try {
            KosProductInquiryResponse response = kosMockService.processProductInquiry(request);
            
            if ("0000".equals(response.getResultCode())) {
                return ResponseEntity.ok(KosCommonResponse.success(response, "가입상품 조회가 완료되었습니다"));
            } else {
                return ResponseEntity.ok(KosCommonResponse.failure(
                        response.getResultCode(), response.getResultMessage()));
            }
            
        } catch (Exception e) {
            log.error("가입상품 조회 처리 중 오류 발생 - RequestId: {}", request.getRequestId(), e);
            return ResponseEntity.ok(KosCommonResponse.systemError());
        }
    }

    /**
     * 회선번호 형식 변환 (01012345678 → 010-1234-5678)
     */
    private String formatLineNumber(String lineNumber) {
        if (lineNumber == null || lineNumber.length() != 11) {
            return lineNumber;
        }
        
        return lineNumber.substring(0, 3) + "-" + 
               lineNumber.substring(3, 7) + "-" + 
               lineNumber.substring(7, 11);
    }

}