package com.phonebill.bill.service;

import com.phonebill.bill.dto.*;
import com.phonebill.bill.exception.BillInquiryException;
import com.phonebill.bill.repository.BillInquiryHistoryRepository;
import com.phonebill.bill.repository.entity.BillInquiryHistoryEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 요금조회 이력 관리 서비스
 * 
 * 요금조회 요청 및 처리 이력을 관리하는 서비스
 * - 비동기 이력 저장으로 응답 성능에 영향 없음
 * - 페이징 처리로 대용량 이력 데이터 효율적 조회
 * - 다양한 필터 조건 지원
 * - 사용자별 권한 기반 이력 접근 제어
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillHistoryService {

    private final BillInquiryHistoryRepository historyRepository;

    /**
     * 요금조회 이력 비동기 저장
     * 
     * 응답 성능에 영향을 주지 않도록 비동기로 처리
     * 
     * @param requestId 요청 ID
     * @param request 요금조회 요청 데이터
     * @param response 요금조회 응답 데이터
     */
    @Async
    @Transactional
    public void saveInquiryHistoryAsync(String requestId, BillInquiryRequest request, BillInquiryResponse response) {
        log.debug("요금조회 이력 비동기 저장 시작 - 요청ID: {}", requestId);

        try {
            // 조회월 기본값 설정
            String inquiryMonth = request.getInquiryMonth();
            if (inquiryMonth == null || inquiryMonth.trim().isEmpty()) {
                inquiryMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            }

            // 결과 요약 생성
            String resultSummary = generateResultSummary(response);

            // 이력 엔티티 생성
            BillInquiryHistoryEntity historyEntity = BillInquiryHistoryEntity.builder()
                    .requestId(requestId)
                    .lineNumber(request.getLineNumber())
                    .inquiryMonth(inquiryMonth)
                    .requestTime(LocalDateTime.now())
                    .processTime(LocalDateTime.now())
                    .status(response.getStatus().name())
                    .resultSummary(resultSummary)
                    .build();

            historyRepository.save(historyEntity);

            log.info("요금조회 이력 저장 완료 - 요청ID: {}, 상태: {}", requestId, response.getStatus());

        } catch (Exception e) {
            log.error("요금조회 이력 저장 오류 - 요청ID: {}, 오류: {}", requestId, e.getMessage(), e);
            // 이력 저장 실패는 전체 프로세스에 영향을 주지 않도록 예외를 던지지 않음
        }
    }

    /**
     * 요금조회 상태 업데이트
     * 
     * 비동기 처리된 요청의 상태가 변경되었을 때 호출
     * 
     * @param requestId 요청 ID
     * @param response 업데이트된 응답 데이터
     */
    @Transactional
    public void updateInquiryStatus(String requestId, BillInquiryResponse response) {
        log.debug("요금조회 상태 업데이트 - 요청ID: {}, 상태: {}", requestId, response.getStatus());

        try {
            BillInquiryHistoryEntity historyEntity = historyRepository.findByRequestId(requestId)
                    .orElseThrow(() -> BillInquiryException.billDataNotFound(requestId, "요청 ID"));

            // 상태 업데이트
            historyEntity.updateStatus(response.getStatus().name());
            historyEntity.updateProcessTime(LocalDateTime.now());

            // 결과 요약 업데이트
            String resultSummary = generateResultSummary(response);
            historyEntity.updateResultSummary(resultSummary);

            historyRepository.save(historyEntity);

            log.info("요금조회 상태 업데이트 완료 - 요청ID: {}, 상태: {}", requestId, response.getStatus());

        } catch (Exception e) {
            log.error("요금조회 상태 업데이트 오류 - 요청ID: {}, 오류: {}", requestId, e.getMessage(), e);
        }
    }

    /**
     * 요금조회 결과 조회
     * 
     * @param requestId 요청 ID
     * @return 요금조회 응답 데이터
     */
    public BillInquiryResponse getBillInquiryResult(String requestId) {
        log.debug("요금조회 결과 조회 - 요청ID: {}", requestId);

        try {
            BillInquiryHistoryEntity historyEntity = historyRepository.findByRequestId(requestId)
                    .orElse(null);

            if (historyEntity == null) {
                log.debug("요금조회 결과 없음 - 요청ID: {}", requestId);
                return null;
            }

            BillInquiryResponse.ProcessStatus status = BillInquiryResponse.ProcessStatus.valueOf(historyEntity.getStatus());

            BillInquiryResponse response = BillInquiryResponse.builder()
                    .requestId(requestId)
                    .status(status)
                    .build();

            // 성공 상태이고 요금 정보가 있는 경우 (실제로는 별도 테이블에서 조회해야 함)
            if (status == BillInquiryResponse.ProcessStatus.COMPLETED) {
                // TODO: 실제 요금 정보 조회 로직 구현
                // 현재는 결과 요약만 반환
            }

            log.debug("요금조회 결과 조회 완료 - 요청ID: {}, 상태: {}", requestId, status);
            return response;

        } catch (Exception e) {
            log.error("요금조회 결과 조회 오류 - 요청ID: {}, 오류: {}", requestId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 요금조회 이력 목록 조회
     * 
     * @param userLineNumbers 사용자 권한이 있는 회선번호 목록
     * @param lineNumber 특정 회선번호 필터 (선택)
     * @param startDate 조회 시작일 (선택)
     * @param endDate 조회 종료일 (선택)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status 상태 필터 (선택)
     * @return 이력 응답 데이터
     */
    public BillHistoryResponse getBillHistory(
            List<String> userLineNumbers, String lineNumber, String startDate, String endDate,
            Integer page, Integer size, BillInquiryResponse.ProcessStatus status) {

        log.debug("요금조회 이력 목록 조회 - 사용자 회선수: {}, 필터 회선: {}, 기간: {} ~ {}, 페이지: {}/{}", 
                userLineNumbers.size(), lineNumber, startDate, endDate, page, size);

        try {
            // 페이징 설정 (최신순 정렬)
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by("requestTime").descending());

            // 검색 조건 설정
            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;

            if (startDate != null && !startDate.trim().isEmpty()) {
                startDateTime = LocalDate.parse(startDate).atStartOfDay();
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59);
            }

            // 조건에 따라 적절한 쿼리 선택
            Page<BillInquiryHistoryEntity> historyPage = getBillHistoryByConditions(
                    userLineNumbers, lineNumber, startDateTime, endDateTime, status, pageable
            );

            // 응답 데이터 변환
            List<BillHistoryResponse.BillHistoryItem> historyItems = historyPage.getContent()
                    .stream()
                    .map(this::convertToHistoryItem)
                    .collect(Collectors.toList());

            // 페이징 정보 구성
            BillHistoryResponse.PaginationInfo paginationInfo = BillHistoryResponse.PaginationInfo.builder()
                    .currentPage(page)
                    .totalPages(historyPage.getTotalPages())
                    .totalItems(historyPage.getTotalElements())
                    .pageSize(size)
                    .hasNext(historyPage.hasNext())
                    .hasPrevious(historyPage.hasPrevious())
                    .build();

            BillHistoryResponse response = BillHistoryResponse.builder()
                    .items(historyItems)
                    .pagination(paginationInfo)
                    .build();

            log.info("요금조회 이력 목록 조회 완료 - 총 {}건, 현재 페이지: {}/{}",
                    historyPage.getTotalElements(), page, historyPage.getTotalPages());

            return response;

        } catch (Exception e) {
            log.error("요금조회 이력 목록 조회 오류 - 오류: {}", e.getMessage(), e);
            throw new BillInquiryException("이력 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 조건에 따라 적절한 쿼리를 선택하여 이력 조회
     */
    private Page<BillInquiryHistoryEntity> getBillHistoryByConditions(
            List<String> userLineNumbers, String lineNumber, 
            LocalDateTime startDateTime, LocalDateTime endDateTime, 
            BillInquiryResponse.ProcessStatus status, Pageable pageable) {
        
        boolean hasLineNumber = lineNumber != null && !lineNumber.trim().isEmpty();
        boolean hasDateRange = startDateTime != null && endDateTime != null;
        boolean hasStatus = status != null;
        
        String statusFilter = hasStatus ? status.name() : null;
        
        // 8가지 경우의 수에 따라 적절한 쿼리 선택
        if (hasLineNumber && hasDateRange && hasStatus) {
            // 모든 필터 적용
            return historyRepository.findBillHistoryWithAllFilters(
                    userLineNumbers, lineNumber, startDateTime, endDateTime, statusFilter, pageable
            );
        } else if (hasLineNumber && hasDateRange) {
            // 회선번호 + 날짜 범위
            return historyRepository.findBillHistoryByLineNumbersAndLineNumberAndDateRange(
                    userLineNumbers, lineNumber, startDateTime, endDateTime, pageable
            );
        } else if (hasLineNumber && hasStatus) {
            // 회선번호 + 상태
            return historyRepository.findBillHistoryByLineNumbersAndLineNumberAndStatus(
                    userLineNumbers, lineNumber, statusFilter, pageable
            );
        } else if (hasDateRange && hasStatus) {
            // 날짜 범위 + 상태
            return historyRepository.findBillHistoryByLineNumbersAndDateRangeAndStatus(
                    userLineNumbers, startDateTime, endDateTime, statusFilter, pageable
            );
        } else if (hasLineNumber) {
            // 회선번호만
            return historyRepository.findBillHistoryByLineNumbersAndLineNumber(
                    userLineNumbers, lineNumber, pageable
            );
        } else if (hasDateRange) {
            // 날짜 범위만
            return historyRepository.findBillHistoryByLineNumbersAndDateRange(
                    userLineNumbers, startDateTime, endDateTime, pageable
            );
        } else if (hasStatus) {
            // 상태만
            return historyRepository.findBillHistoryByLineNumbersAndStatus(
                    userLineNumbers, statusFilter, pageable
            );
        } else {
            // 필터 없음 (기본)
            return historyRepository.findBillHistoryByLineNumbers(
                    userLineNumbers, pageable
            );
        }
    }

    /**
     * 엔티티를 이력 아이템으로 변환
     */
    private BillHistoryResponse.BillHistoryItem convertToHistoryItem(BillInquiryHistoryEntity entity) {
        BillInquiryResponse.ProcessStatus status = BillInquiryResponse.ProcessStatus.valueOf(entity.getStatus());

        return BillHistoryResponse.BillHistoryItem.builder()
                .requestId(entity.getRequestId())
                .lineNumber(entity.getLineNumber())
                .inquiryMonth(entity.getInquiryMonth())
                .requestTime(entity.getRequestTime())
                .processTime(entity.getProcessTime())
                .status(status)
                .resultSummary(entity.getResultSummary())
                .build();
    }

    /**
     * 응답 데이터를 기반으로 결과 요약 생성
     */
    private String generateResultSummary(BillInquiryResponse response) {
        try {
            switch (response.getStatus()) {
                case COMPLETED:
                    if (response.getBillInfo() != null) {
                        return String.format("%s, %,d원", 
                                response.getBillInfo().getProductName(),
                                response.getBillInfo().getTotalAmount());
                    } else {
                        return "조회 완료";
                    }
                case PROCESSING:
                    return "처리 중";
                case FAILED:
                    return "조회 실패";
                default:
                    return "알 수 없는 상태";
            }
        } catch (Exception e) {
            log.warn("결과 요약 생성 오류: {}", e.getMessage());
            return response.getStatus().name();
        }
    }
}