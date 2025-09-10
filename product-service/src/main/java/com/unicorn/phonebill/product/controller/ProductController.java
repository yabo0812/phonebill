package com.unicorn.phonebill.product.controller;

import com.unicorn.phonebill.product.dto.*;
import com.unicorn.phonebill.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * 상품변경 서비스 REST API 컨트롤러
 * 
 * 주요 기능:
 * - 고객 및 상품 정보 조회 (UFR-PROD-020)
 * - 상품변경 요청 및 사전체크 (UFR-PROD-030)
 * - KOS 연동 상품변경 처리 (UFR-PROD-040)
 * - 상품변경 이력 조회
 */
@RestController
@RequestMapping("/api/v1/products")
@Validated
@Tag(name = "Product Change Service", description = "상품변경 서비스 API")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }


    /**
     * 고객 정보 조회
     * UFR-PROD-020 구현
     */
    @GetMapping("/customer")
    @Operation(summary = "고객 정보 조회", 
               description = "특정 회선번호의 고객 정보와 현재 상품 정보를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "고객 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomerInfoResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "고객 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CustomerInfoResponse> getCustomerInfo(
            @Parameter(description = "고객 회선번호", example = "01012345678")
            @RequestParam("lineNumber")
            String lineNumber) {
        
        String userId = getCurrentUserId();
        
        // 회선번호에서 대시 제거
        String normalizedLineNumber = lineNumber.replaceAll("-", "");
        
        // 정규화된 회선번호 유효성 검증
        if (!normalizedLineNumber.matches("^010[0-9]{8}$")) {
            throw new IllegalArgumentException("회선번호는 010으로 시작하는 11자리 숫자여야 합니다");
        }
        
        logger.info("고객 정보 조회 요청: lineNumber={} (original: {}), userId={}", 
                   normalizedLineNumber, lineNumber, userId);

        try {
            CustomerInfoResponse response = productService.getCustomerInfo(normalizedLineNumber);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("고객 정보 조회 실패: lineNumber={}, userId={}", lineNumber, userId, e);
            throw new RuntimeException("고객 정보 조회 중 오류가 발생했습니다");
        }
    }

    /**
     * 변경 가능한 상품 목록 조회
     * UFR-PROD-020 구현
     */
    @GetMapping("/available")
    @Operation(summary = "변경 가능한 상품 목록 조회", 
               description = "현재 판매중이고 변경 가능한 상품 목록을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상품 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = AvailableProductsResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AvailableProductsResponse> getAvailableProducts(
            @Parameter(description = "현재 상품코드 (필터링용)")
            @RequestParam(required = false) String currentProductCode) {
        
        String userId = getCurrentUserId();
        logger.info("가용 상품 목록 조회 요청: currentProductCode={}, userId={}", 
                   currentProductCode, userId);

        try {
            AvailableProductsResponse response = productService.getAvailableProducts(currentProductCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("가용 상품 목록 조회 실패: currentProductCode={}, userId={}", 
                        currentProductCode, userId, e);
            throw new RuntimeException("상품 목록 조회 중 오류가 발생했습니다");
        }
    }

    /**
     * 상품변경 사전체크
     * UFR-PROD-030 구현
     */
    @PostMapping("/change/validation")
    @Operation(summary = "상품변경 사전체크", 
               description = "상품변경 요청 전 사전체크를 수행합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "사전체크 완료 (성공/실패 포함)",
                    content = @Content(schema = @Schema(implementation = ProductChangeValidationResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductChangeValidationResponse> validateProductChange(
            @Valid @RequestBody ProductChangeValidationRequest request) {
        
        String userId = getCurrentUserId();
        logger.info("상품변경 사전체크 요청: lineNumber={}, current={}, target={}, userId={}", 
                   request.getLineNumber(), request.getCurrentProductCode(), 
                   request.getTargetProductCode(), userId);

        try {
            ProductChangeValidationResponse response = productService.validateProductChange(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("상품변경 사전체크 실패: lineNumber={}, userId={}", request.getLineNumber(), userId, e);
            throw new RuntimeException("상품변경 사전체크 중 오류가 발생했습니다");
        }
    }

    /**
     * 상품변경 요청 (동기 처리)
     * UFR-PROD-040 구현
     */
    @PostMapping("/change")
    @Operation(summary = "상품변경 요청", 
               description = "실제 상품변경 처리를 요청합니다 (동기 처리)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상품변경 처리 완료",
                    content = @Content(schema = @Schema(implementation = ProductChangeResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "사전체크 실패 또는 처리 불가 상태",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "KOS 시스템 장애 (Circuit Breaker Open)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductChangeResponse> requestProductChange(
            @Valid @RequestBody ProductChangeRequest request) {
        
        String userId = getCurrentUserId();
        logger.info("상품변경 요청: lineNumber={}, current={}, target={}, userId={}", 
                   request.getLineNumber(), request.getCurrentProductCode(), 
                   request.getTargetProductCode(), userId);

        try {
            // 동기 처리
            ProductChangeResponse response = productService.requestProductChange(request, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("상품변경 요청 실패: lineNumber={}, userId={}", request.getLineNumber(), userId, e);
            throw new RuntimeException("상품변경 처리 중 오류가 발생했습니다");
        }
    }


    /**
     * 상품변경 이력 조회
     * UFR-PROD-040 구현 (이력 관리)
     */
    @GetMapping("/history")
    @Operation(summary = "상품변경 이력 조회", 
               description = "고객의 상품변경 이력을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "이력 조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductChangeHistoryResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductChangeHistoryResponse> getProductChangeHistory(
            @Parameter(description = "회선번호 (미입력시 로그인 고객 기준)")
            @RequestParam(required = false) 
            String lineNumber,
            @Parameter(description = "조회 시작일 (YYYY-MM-DD)")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "조회 종료일 (YYYY-MM-DD)")
            @RequestParam(required = false) String endDate,
            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "10") int size) {
        
        String userId = getCurrentUserId();
        
        // 회선번호 정규화 (입력된 경우에만)
        String normalizedLineNumber = null;
        if (lineNumber != null && !lineNumber.trim().isEmpty()) {
            normalizedLineNumber = lineNumber.replaceAll("-", "");
            
            // 정규화된 회선번호 유효성 검증
            if (!normalizedLineNumber.matches("^010[0-9]{8}$")) {
                throw new IllegalArgumentException("회선번호는 010으로 시작하는 11자리 숫자여야 합니다");
            }
        }
        
        logger.info("상품변경 이력 조회 요청: lineNumber={} (original: {}), startDate={}, endDate={}, page={}, size={}, userId={}", 
                   normalizedLineNumber, lineNumber, startDate, endDate, page, size, userId);

        try {
            // 페이지 번호를 0-based로 변환
            Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, size)));
            
            // 날짜 유효성 검증
            validateDateRange(startDate, endDate);
            
            ProductChangeHistoryResponse response = productService.getProductChangeHistory(
                normalizedLineNumber, startDate, endDate, pageable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("상품변경 이력 조회 실패: lineNumber={}, userId={}", lineNumber, userId, e);
            throw new RuntimeException("상품변경 이력 조회 중 오류가 발생했습니다");
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * 현재 인증된 사용자 ID 조회
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new RuntimeException("인증된 사용자 정보를 찾을 수 없습니다");
    }

    /**
     * 날짜 범위 유효성 검증
     */
    private void validateDateRange(String startDate, String endDate) {
        if (startDate != null && endDate != null) {
            try {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                
                if (start.isAfter(end)) {
                    throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다");
                }
                
                if (start.isBefore(LocalDate.now().minusYears(2))) {
                    throw new IllegalArgumentException("조회 가능한 기간을 초과했습니다 (최대 2년)");
                }
            } catch (Exception e) {
                if (e instanceof IllegalArgumentException) {
                    throw e;
                }
                throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다 (YYYY-MM-DD)");
            }
        }
    }

    // ========== Exception Handler ==========

    /**
     * 컨트롤러 레벨 예외 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        logger.error("컨트롤러에서 런타임 예외 발생", e);
        ErrorResponse errorResponse = ErrorResponse.internalServerError(e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("잘못된 요청 파라미터: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.validationError(e.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }
}