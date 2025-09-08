package com.phonebill.bill.service;

import com.phonebill.bill.dto.*;

/**
 * 요금조회 서비스 인터페이스
 * 
 * 통신요금 조회와 관련된 비즈니스 로직을 정의
 * - 요금조회 메뉴 데이터 제공
 * - KOS 시스템 연동을 통한 실시간 요금 조회
 * - 요금조회 결과 상태 관리
 * - 요금조회 이력 관리
 * 
 * @author 이개발(백엔더)
 * @version 1.0.0
 * @since 2025-09-08
 */
public interface BillInquiryService {

    /**
     * 요금조회 메뉴 조회
     * 
     * UFR-BILL-010: 요금조회 메뉴 접근
     * - 인증된 사용자의 고객정보 조회
     * - 조회 가능한 월 목록 생성 (최근 12개월)
     * - 현재 월 정보 제공
     * 
     * @return 요금조회 메뉴 응답 데이터
     */
    BillMenuResponse getBillMenu();

    /**
     * 요금조회 요청 처리
     * 
     * UFR-BILL-020: 요금조회 신청
     * - Cache-Aside 패턴으로 캐시 확인
     * - 캐시 Miss 시 KOS 시스템 연동
     * - Circuit Breaker 패턴으로 장애 격리
     * - 비동기 처리 시 요청 상태 관리
     * 
     * @param request 요금조회 요청 데이터
     * @return 요금조회 응답 데이터
     */
    BillInquiryResponse inquireBill(BillInquiryRequest request);

    /**
     * 요금조회 결과 확인
     * 
     * 비동기로 처리된 요금조회의 상태와 결과를 반환
     * - PROCESSING: 처리 중 상태
     * - COMPLETED: 처리 완료 (요금 정보 포함)
     * - FAILED: 처리 실패 (오류 메시지 포함)
     * 
     * @param requestId 요금조회 요청 ID
     * @return 요금조회 응답 데이터
     */
    BillInquiryResponse getBillInquiryResult(String requestId);

    /**
     * 요금조회 이력 조회
     * 
     * UFR-BILL-040: 요금조회 결과 전송 및 이력 관리
     * - 사용자별 요금조회 이력 목록 조회
     * - 필터링: 회선번호, 기간, 상태
     * - 페이징 처리
     * 
     * @param lineNumber 회선번호 (선택)
     * @param startDate 조회 시작일 (선택)
     * @param endDate 조회 종료일 (선택)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status 처리 상태 필터 (선택)
     * @return 요금조회 이력 응답 데이터
     */
    BillHistoryResponse getBillHistory(
        String lineNumber, 
        String startDate, 
        String endDate, 
        Integer page, 
        Integer size, 
        BillInquiryResponse.ProcessStatus status
    );
}