package com.phonebill.kosmock.controller;

import com.phonebill.kosmock.dto.MockDataCreateRequest;
import com.phonebill.kosmock.dto.MockDataCreateResponse;
import com.phonebill.kosmock.service.MockDataCreateService;
import com.phonebill.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Mock 데이터 생성 및 조회 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/kos/mock-datas")
@RequiredArgsConstructor
@Tag(name = "Mock Data Management", description = "Mock 데이터 생성, 조회 및 관리 API")
public class MockDataController {
    
    private static final Logger log = LoggerFactory.getLogger(MockDataController.class);
    private final MockDataCreateService mockDataCreateService;
    
    @PostMapping
    @Operation(summary = "Mock 데이터 생성", description = "고객 정보와 요금 정보 Mock 데이터를 생성합니다")
    public ResponseEntity<ApiResponse<MockDataCreateResponse>> createMockData(
            @Valid @RequestBody MockDataCreateRequest request) {
        
        log.info("Mock 데이터 생성 요청 - CustomerId: {}, LineNumber: {}", 
            request.getCustomerId(), request.getLineNumber());
        
        try {
            MockDataCreateResponse response = mockDataCreateService.createMockData(request);
            
            return ResponseEntity.ok(ApiResponse.success("Mock 데이터가 성공적으로 생성되었습니다", response));
            
        } catch (IllegalArgumentException e) {
            log.warn("Mock 데이터 생성 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage(), "4000"));
                
        } catch (IllegalStateException e) {
            log.error("Mock 데이터 생성 실패 - 시스템 상태 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(e.getMessage(), "5000"));
                
        } catch (Exception e) {
            log.error("Mock 데이터 생성 실패 - 예기치 못한 오류", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Mock 데이터 생성 중 오류가 발생했습니다", "5000"));
        }
    }
    
    /**
     * 가입상품정보 조회 API
     */
    @GetMapping("/customer/product")
    @Operation(summary = "가입상품정보 조회", description = "고객 ID와 회선번호로 가입상품정보를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "고객 정보를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Object>> getCustomerProduct(
            @Parameter(description = "고객 ID", example = "CUST_001", required = true)
            @RequestParam String customerId,
            @Parameter(description = "회선번호", example = "01012345679", required = true)
            @RequestParam String lineNumber) {
        
        log.info("가입상품정보 조회 요청 - CustomerId: {}, LineNumber: {}", customerId, lineNumber);
        
        try {
            Object productInfo = mockDataCreateService.getCustomerProduct(customerId, lineNumber);
            
            if (productInfo != null) {
                return ResponseEntity.ok(ApiResponse.success("가입상품정보 조회가 완료되었습니다", productInfo));
            } else {
                return ResponseEntity.ok(ApiResponse.error("해당 고객의 상품정보를 찾을 수 없습니다", "1001"));
            }
            
        } catch (Exception e) {
            log.error("가입상품정보 조회 중 오류 발생 - CustomerId: {}, LineNumber: {}", 
                    customerId, lineNumber, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("가입상품정보 조회 중 오류가 발생했습니다", "5000"));
        }
    }

    /**
     * 요금정보 조회 API
     */
    @GetMapping("/customer/bill")
    @Operation(summary = "요금정보 조회", description = "고객 ID와 회선번호로 요금정보를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", 
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "고객 정보를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Object>> getCustomerBill(
            @Parameter(description = "고객 ID", example = "CUST_001", required = true)
            @RequestParam String customerId,
            @Parameter(description = "회선번호", example = "01012345679", required = true)
            @RequestParam String lineNumber) {
        
        log.info("요금정보 조회 요청 - CustomerId: {}, LineNumber: {}", customerId, lineNumber);
        
        try {
            Object billInfo = mockDataCreateService.getCustomerBill(customerId, lineNumber);
            
            if (billInfo != null) {
                return ResponseEntity.ok(ApiResponse.success("요금정보 조회가 완료되었습니다", billInfo));
            } else {
                return ResponseEntity.ok(ApiResponse.error("해당 고객의 요금정보를 찾을 수 없습니다", "1002"));
            }
            
        } catch (Exception e) {
            log.error("요금정보 조회 중 오류 발생 - CustomerId: {}, LineNumber: {}", 
                    customerId, lineNumber, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("요금정보 조회 중 오류가 발생했습니다", "5000"));
        }
    }
}