package com.phonebill.bill.service;

import com.phonebill.bill.dto.*;
import com.phonebill.bill.exception.BillInquiryException;
import com.phonebill.common.security.UserPrincipal;
import com.phonebill.kosmock.dto.KosBillInquiryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        String customerId = getCurrentCustomerId();
        String lineNumber = getCurrentLineNumber();

        // 실제 요금 데이터가 있는 월 목록 조회
        List<String> availableMonths = getAvailableMonthsWithData(lineNumber);
        
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
    public KosBillInquiryResponse inquireBill(BillInquiryRequest request) {
        log.info("요금조회 요청 처리 시작 - 회선: {}, 조회월: {}", 
                request.getLineNumber(), request.getInquiryMonth());

        // 요청 ID 생성
        String requestId = generateRequestId();

        // 조회월 기본값 설정 (미입력시 당월)
        String inquiryMonth = request.getInquiryMonth();
        if (inquiryMonth == null || inquiryMonth.trim().isEmpty()) {
            inquiryMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        }

        try {
            // KOS Mock 서비스 직접 호출
            KosBillInquiryResponse response = kosClientService.inquireBillFromKosDirect(
                request.getLineNumber(), inquiryMonth
            );

            log.info("KOS Mock 요금조회 완료 - 요청ID: {}, 상태: {}", 
                    response.getRequestId(), response.getProcStatus());
            return response;

        } catch (Exception e) {
            log.error("KOS Mock 요금조회 실패 - 회선: {}, 오류: {}", 
                    request.getLineNumber(), e.getMessage(), e);
            
            // 실패 시 기본 응답 반환
            return KosBillInquiryResponse.builder()
                    .requestId(requestId)
                    .procStatus("FAILED")
                    .resultCode("9999")
                    .resultMessage("요금 조회 중 오류가 발생했습니다")
                    .build();
        }
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
        // JWT에서 인증된 사용자의 고객 ID 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String customerId = userPrincipal.getCustomerId();
            
            if (customerId != null && !customerId.trim().isEmpty()) {
                log.debug("사용자 {}의 고객 ID: {}", userPrincipal.getUserId(), customerId);
                return customerId;
            }
        }
        
        // 인증 정보가 없거나 고객 ID가 없는 경우 기본값 반환
        log.warn("사용자의 고객 ID 정보를 찾을 수 없습니다. 기본값 사용");
        return "CUST001";
    }

    /**
     * 현재 인증된 사용자의 회선번호 조회
     */
    private String getCurrentLineNumber() {
        // JWT에서 인증된 사용자의 회선번호 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String lineNumber = userPrincipal.getLineNumber();
            
            if (lineNumber != null && !lineNumber.trim().isEmpty()) {
                log.debug("사용자 {}의 회선번호: {}", userPrincipal.getUserId(), lineNumber);
                return lineNumber;
            }
        }
        
        // 인증 정보가 없거나 회선번호가 없는 경우 기본값 반환
        log.warn("사용자의 회선번호 정보를 찾을 수 없습니다. 기본값 사용");
        return "010-1234-5678";
    }

    /**
     * 현재 사용자의 모든 회선번호 목록 조회
     */
    private List<String> getCurrentUserLineNumbers() {
        // JWT에서 인증된 사용자의 회선번호 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String lineNumber = userPrincipal.getLineNumber();
            
            if (lineNumber != null) {
                List<String> lineNumbers = new ArrayList<>();
                lineNumbers.add(lineNumber);
                log.debug("사용자 {}의 회선번호: {}", userPrincipal.getUserId(), lineNumber);
                return lineNumbers;
            }
        }
        
        // 인증 정보가 없거나 회선번호가 없는 경우 빈 목록 반환
        log.warn("사용자의 회선번호 정보를 찾을 수 없습니다");
        return new ArrayList<>();
    }

    /**
     * 실제 요금 데이터가 있는 월 목록 조회
     */
    private List<String> getAvailableMonthsWithData(String lineNumber) {
        try {
            log.info("회선 {}의 실제 요금 데이터가 있는 월 목록 조회", lineNumber);
            
            // KOS Mock 서비스를 통해 실제 데이터가 있는 월 목록 조회
            List<String> availableMonths = kosClientService.getAvailableMonths(lineNumber);
            
            if (availableMonths == null || availableMonths.isEmpty()) {
                log.warn("KOS에서 조회 가능한 월 정보가 없음. 기본 최근 3개월 반환");
                return generateDefaultMonths(3);
            }
            
            log.info("KOS에서 조회된 데이터 보유 월: {}", availableMonths);
            return availableMonths;
            
        } catch (Exception e) {
            log.error("KOS 시스템에서 조회 가능한 월 정보 조회 실패: {}", e.getMessage(), e);
            // 실패 시 기본 최근 3개월 반환
            return generateDefaultMonths(3);
        }
    }

    /**
     * 기본 월 목록 생성 (fallback용)
     */
    private List<String> generateDefaultMonths(int monthCount) {
        List<String> months = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 0; i < monthCount; i++) {
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