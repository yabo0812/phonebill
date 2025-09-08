package com.phonebill.bill.service;

import com.phonebill.bill.dto.*;
import com.phonebill.bill.exception.BillInquiryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 요금조회 서비스 구현체
 * 
 * 통신요금 조회와 관련된 비즈니스 로직 구현
 * - KOS 시스템 연동을 통한 실시간 데이터 조회
 * - Redis 캐싱을 통한 성능 최적화
 * - Circuit Breaker를 통한 외부 시스템 장애 격리
 * - 비동기 처리 및 이력 관리
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillInquiryServiceImpl implements BillInquiryService {

    private final BillCacheService billCacheService;
    private final KosClientService kosClientService;
    private final BillHistoryService billHistoryService;

    /**
     * 요금조회 메뉴 조회
     * 
     * UFR-BILL-010: 요금조회 메뉴 접근
     */
    @Override
    public BillMenuResponse getBillMenu() {
        log.info("요금조회 메뉴 조회 시작");

        // 현재 인증된 사용자의 고객 정보 조회 (JWT에서 추출)
        // TODO: SecurityContext에서 사용자 정보 추출 로직 구현
        String customerId = getCurrentCustomerId();
        String lineNumber = getCurrentLineNumber();

        // 조회 가능한 월 목록 생성 (최근 12개월)
        List<String> availableMonths = generateAvailableMonths();
        
        // 현재 월
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        BillMenuResponse response = BillMenuResponse.builder()
                .customerInfo(BillMenuResponse.CustomerInfo.builder()
                        .customerId(customerId)
                        .lineNumber(lineNumber)
                        .build())
                .availableMonths(availableMonths)
                .currentMonth(currentMonth)
                .build();

        log.info("요금조회 메뉴 조회 완료 - 고객: {}, 회선: {}", customerId, lineNumber);
        return response;
    }

    /**
     * 요금조회 요청 처리
     * 
     * UFR-BILL-020: 요금조회 신청
     */
    @Override
    @Transactional
    public BillInquiryResponse inquireBill(BillInquiryRequest request) {
        log.info("요금조회 요청 처리 시작 - 회선: {}, 조회월: {}", 
                request.getLineNumber(), request.getInquiryMonth());

        // 요청 ID 생성
        String requestId = generateRequestId();

        // 조회월 기본값 설정 (미입력시 당월)
        String inquiryMonth = request.getInquiryMonth();
        if (inquiryMonth == null || inquiryMonth.trim().isEmpty()) {
            inquiryMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        try {
            // 1단계: 캐시에서 데이터 확인 (Cache-Aside 패턴)
            BillInquiryResponse cachedResponse = billCacheService.getCachedBillData(
                request.getLineNumber(), inquiryMonth
            );

            if (cachedResponse != null) {
                log.info("캐시에서 요금 데이터 조회 완료 - 요청ID: {}", requestId);
                cachedResponse = BillInquiryResponse.builder()
                        .requestId(requestId)
                        .status(BillInquiryResponse.ProcessStatus.COMPLETED)
                        .billInfo(cachedResponse.getBillInfo())
                        .build();
                
                // 이력 저장 (비동기)
                billHistoryService.saveInquiryHistoryAsync(requestId, request, cachedResponse);
                return cachedResponse;
            }

            // 2단계: KOS 시스템 연동 (Circuit Breaker 적용)
            CompletableFuture<BillInquiryResponse> kosResponseFuture = kosClientService.inquireBillFromKos(
                request.getLineNumber(), inquiryMonth
            );
            BillInquiryResponse kosResponse;
            try {
                kosResponse = kosResponseFuture.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BillInquiryException("요금조회 처리가 중단되었습니다", e);
            } catch (Exception e) {
                throw new BillInquiryException("요금조회 처리 중 오류가 발생했습니다", e);
            }

            if (kosResponse != null && kosResponse.getStatus() == BillInquiryResponse.ProcessStatus.COMPLETED) {
                // 3단계: 캐시에 저장 (1시간 TTL)
                billCacheService.cacheBillData(request.getLineNumber(), inquiryMonth, kosResponse);

                // 응답 데이터 구성
                BillInquiryResponse response = BillInquiryResponse.builder()
                        .requestId(requestId)
                        .status(BillInquiryResponse.ProcessStatus.COMPLETED)
                        .billInfo(kosResponse.getBillInfo())
                        .build();

                // 이력 저장 (비동기)
                billHistoryService.saveInquiryHistoryAsync(requestId, request, response);

                log.info("KOS 연동을 통한 요금조회 완료 - 요청ID: {}", requestId);
                return response;
            } else {
                // KOS에서 비동기 처리 중인 경우
                BillInquiryResponse response = BillInquiryResponse.builder()
                        .requestId(requestId)
                        .status(BillInquiryResponse.ProcessStatus.PROCESSING)
                        .build();

                // 이력 저장 (처리 중 상태)
                billHistoryService.saveInquiryHistoryAsync(requestId, request, response);

                log.info("KOS 연동 비동기 처리 - 요청ID: {}", requestId);
                return response;
            }

        } catch (Exception e) {
            log.error("요금조회 처리 중 오류 발생 - 요청ID: {}, 오류: {}", requestId, e.getMessage(), e);
            
            // 실패 응답 생성
            BillInquiryResponse errorResponse = BillInquiryResponse.builder()
                    .requestId(requestId)
                    .status(BillInquiryResponse.ProcessStatus.FAILED)
                    .build();
            
            // 이력 저장 (실패 상태)
            billHistoryService.saveInquiryHistoryAsync(requestId, request, errorResponse);
            
            // 비즈니스 예외는 그대로 던지고, 시스템 예외는 래핑
            if (e instanceof BillInquiryException) {
                throw e;
            } else {
                throw new BillInquiryException("요금조회 처리 중 시스템 오류가 발생했습니다", e);
            }
        }
    }

    /**
     * 요금조회 결과 확인
     */
    @Override
    public BillInquiryResponse getBillInquiryResult(String requestId) {
        log.info("요금조회 결과 확인 - 요청ID: {}", requestId);

        // 이력에서 요청 정보 조회
        BillInquiryResponse response = billHistoryService.getBillInquiryResult(requestId);
        
        if (response == null) {
            throw BillInquiryException.billDataNotFound(requestId, "요청 ID");
        }

        // 처리 중인 경우 KOS에서 최신 상태 확인
        if (response.getStatus() == BillInquiryResponse.ProcessStatus.PROCESSING) {
            try {
                BillInquiryResponse latestResponse = kosClientService.checkInquiryStatus(requestId);
                if (latestResponse != null) {
                    // 상태 업데이트
                    billHistoryService.updateInquiryStatus(requestId, latestResponse);
                    response = latestResponse;
                }
            } catch (Exception e) {
                log.warn("KOS 상태 확인 중 오류 발생 - 요청ID: {}, 오류: {}", requestId, e.getMessage());
                // 상태 확인 실패해도 기존 상태 그대로 반환
            }
        }

        log.info("요금조회 결과 반환 - 요청ID: {}, 상태: {}", requestId, response.getStatus());
        return response;
    }

    /**
     * 요금조회 이력 조회
     */
    @Override
    public BillHistoryResponse getBillHistory(
            String lineNumber, String startDate, String endDate, 
            Integer page, Integer size, BillInquiryResponse.ProcessStatus status) {
        
        log.info("요금조회 이력 조회 - 회선: {}, 기간: {} ~ {}, 페이지: {}/{}, 상태: {}", 
                lineNumber, startDate, endDate, page, size, status);

        // 현재 사용자의 회선번호 목록 조회 (권한 확인)
        List<String> userLineNumbers = getCurrentUserLineNumbers();
        
        // 지정된 회선번호가 사용자 소유가 아닌 경우 권한 오류
        if (lineNumber != null && !userLineNumbers.contains(lineNumber)) {
            throw new BillInquiryException("UNAUTHORIZED_LINE_NUMBER", 
                    "조회 권한이 없는 회선번호입니다", "회선번호: " + lineNumber);
        }

        // 이력 조회 (사용자 권한 기반)
        BillHistoryResponse historyResponse = billHistoryService.getBillHistory(
            userLineNumbers, lineNumber, startDate, endDate, page, size, status
        );

        log.info("요금조회 이력 조회 완료 - 총 {}건", 
                historyResponse.getPagination().getTotalItems());
        
        return historyResponse;
    }

    // === Private Helper Methods ===

    /**
     * 현재 인증된 사용자의 고객 ID 조회
     */
    private String getCurrentCustomerId() {
        // TODO: SecurityContext에서 JWT 토큰을 파싱하여 고객 ID 추출
        // 현재는 더미 데이터 반환
        return "CUST001";
    }

    /**
     * 현재 인증된 사용자의 회선번호 조회
     */
    private String getCurrentLineNumber() {
        // TODO: SecurityContext에서 JWT 토큰을 파싱하여 회선번호 추출
        // 현재는 더미 데이터 반환
        return "010-1234-5678";
    }

    /**
     * 현재 사용자의 모든 회선번호 목록 조회
     */
    private List<String> getCurrentUserLineNumbers() {
        // TODO: 사용자 권한에 따른 회선번호 목록 조회
        // 현재는 더미 데이터 반환
        List<String> lineNumbers = new ArrayList<>();
        lineNumbers.add("010-1234-5678");
        return lineNumbers;
    }

    /**
     * 조회 가능한 월 목록 생성 (최근 12개월)
     */
    private List<String> generateAvailableMonths() {
        List<String> months = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 0; i < 12; i++) {
            LocalDate monthDate = currentDate.minusMonths(i);
            months.add(monthDate.format(formatter));
        }

        return months;
    }

    /**
     * 요청 ID 생성
     */
    private String generateRequestId() {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("REQ_%s_%s", currentDate, uuid);
    }
}