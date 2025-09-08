package com.phonebill.bill.controller;

import com.phonebill.bill.dto.*;
import com.phonebill.bill.service.BillInquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 요금조회 관련 REST API 컨트롤러
 * 
 * 통신요금 조회 서비스의 주요 기능을 제공:
 * - UFR-BILL-010: 요금조회 메뉴 접근
 * - UFR-BILL-020: 요금조회 신청 (동기/비동기 처리)
 * - UFR-BILL-030: 요금조회 결과 확인
 * - UFR-BILL-040: 요금조회 이력 관리
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Slf4j
@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
@Validated
@Tag(name = "Bill Inquiry", description = "요금조회 관련 API")
public class BillController {

    private final BillInquiryService billInquiryService;

    /**
     * 요금조회 메뉴 조회
     * 
     * UFR-BILL-010: 요금조회 메뉴 접근
     * - 고객 회선번호 표시
     * - 조회월 선택 옵션 제공
     * - 요금 조회 신청 버튼 활성화
     */
    @GetMapping("/menu")
    @Operation(
        summary = "요금조회 메뉴 조회",
        description = "요금조회 메뉴 화면에 필요한 정보(고객정보, 조회가능월)를 제공합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요금조회 메뉴 정보 조회 성공",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "서버 내부 오류"
        )
    })
    public ResponseEntity<ApiResponse<BillMenuResponse>> getBillMenu() {
        log.info("요금조회 메뉴 조회 요청");
        
        BillMenuResponse menuData = billInquiryService.getBillMenu();
        
        log.info("요금조회 메뉴 조회 완료 - 고객: {}", menuData.getCustomerInfo().getCustomerId());
        return ResponseEntity.ok(
            ApiResponse.success(menuData, "요금조회 메뉴를 성공적으로 조회했습니다")
        );
    }

    /**
     * 요금조회 요청
     * 
     * UFR-BILL-020: 요금조회 신청
     * - 시나리오 1: 조회월 미선택 (당월 청구요금 조회)
     * - 시나리오 2: 조회월 선택 (특정월 청구요금 조회)
     * 
     * Cache-Aside 패턴과 Circuit Breaker 패턴 적용
     */
    @PostMapping("/inquiry")
    @Operation(
        summary = "요금조회 요청",
        description = "지정된 회선번호와 조회월의 요금 정보를 조회합니다. " +
                     "캐시 확인 후 KOS 시스템 연동을 통해 실시간 데이터를 제공합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요금조회 완료 (동기 처리)",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "202",
            description = "요금조회 요청 접수 (비동기 처리)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 데이터"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "503",
            description = "KOS 시스템 장애 (Circuit Breaker Open)"
        )
    })
    public ResponseEntity<ApiResponse<BillInquiryResponse>> inquireBill(
            @Valid @RequestBody BillInquiryRequest request) {
        log.info("요금조회 요청 - 회선번호: {}, 조회월: {}", 
                request.getLineNumber(), request.getInquiryMonth());
        
        BillInquiryResponse response = billInquiryService.inquireBill(request);
        
        if (response.getStatus() == BillInquiryResponse.ProcessStatus.COMPLETED) {
            log.info("요금조회 완료 - 요청ID: {}, 회선: {}", 
                    response.getRequestId(), request.getLineNumber());
            return ResponseEntity.ok(
                ApiResponse.success(response, "요금조회가 완료되었습니다")
            );
        } else {
            log.info("요금조회 비동기 처리 - 요청ID: {}, 상태: {}", 
                    response.getRequestId(), response.getStatus());
            return ResponseEntity.accepted().body(
                ApiResponse.success(response, "요금조회 요청이 접수되었습니다")
            );
        }
    }

    /**
     * 요금조회 결과 확인
     * 
     * 비동기로 처리된 요금조회 결과를 확인합니다.
     * requestId를 통해 조회 상태와 결과를 반환합니다.
     */
    @GetMapping("/inquiry/{requestId}")
    @Operation(
        summary = "요금조회 결과 확인",
        description = "비동기로 처리된 요금조회의 상태와 결과를 확인합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요금조회 결과 조회 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "요청 ID를 찾을 수 없음"
        )
    })
    public ResponseEntity<ApiResponse<BillInquiryResponse>> getBillInquiryResult(
            @Parameter(description = "요금조회 요청 ID", example = "REQ_20240308_001")
            @PathVariable String requestId) {
        log.info("요금조회 결과 확인 - 요청ID: {}", requestId);
        
        BillInquiryResponse response = billInquiryService.getBillInquiryResult(requestId);
        
        log.info("요금조회 결과 반환 - 요청ID: {}, 상태: {}", requestId, response.getStatus());
        return ResponseEntity.ok(
            ApiResponse.success(response, "요금조회 결과를 조회했습니다")
        );
    }

    /**
     * 요금조회 이력 조회
     * 
     * UFR-BILL-040: 요금조회 결과 전송 및 이력 관리
     * - 요금 조회 요청 이력: MVNO → MP
     * - 요금 조회 처리 이력: MP → KOS
     */
    @GetMapping("/history")
    @Operation(
        summary = "요금조회 이력 조회",
        description = "사용자의 요금조회 요청 및 처리 이력을 페이징으로 제공합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요금조회 이력 조회 성공"
        )
    })
    public ResponseEntity<ApiResponse<BillHistoryResponse>> getBillHistory(
            @Parameter(description = "회선번호 (미입력시 인증된 사용자의 모든 회선)")
            @RequestParam(required = false)
            @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "회선번호 형식이 올바르지 않습니다")
            String lineNumber,
            
            @Parameter(description = "조회 시작일 (YYYY-MM-DD)")
            @RequestParam(required = false)
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식이 올바르지 않습니다 (YYYY-MM-DD)")
            String startDate,
            
            @Parameter(description = "조회 종료일 (YYYY-MM-DD)")  
            @RequestParam(required = false)
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식이 올바르지 않습니다 (YYYY-MM-DD)")
            String endDate,
            
            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(defaultValue = "1") Integer page,
            
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") Integer size,
            
            @Parameter(description = "처리 상태 필터")
            @RequestParam(required = false) BillInquiryResponse.ProcessStatus status) {
        
        log.info("요금조회 이력 조회 - 회선: {}, 기간: {} ~ {}, 페이지: {}/{}", 
                lineNumber, startDate, endDate, page, size);
        
        BillHistoryResponse historyData = billInquiryService.getBillHistory(
            lineNumber, startDate, endDate, page, size, status
        );
        
        log.info("요금조회 이력 조회 완료 - 총 {}건, 페이지: {}/{}",
                historyData.getPagination().getTotalItems(), 
                historyData.getPagination().getCurrentPage(),
                historyData.getPagination().getTotalPages());
        
        return ResponseEntity.ok(
            ApiResponse.success(historyData, "요금조회 이력을 조회했습니다")
        );
    }
}