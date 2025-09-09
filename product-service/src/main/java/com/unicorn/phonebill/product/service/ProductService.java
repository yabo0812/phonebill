package com.unicorn.phonebill.product.service;

import com.unicorn.phonebill.product.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 상품 관리 서비스 인터페이스
 * 
 * 주요 기능:
 * - 고객 및 상품 정보 조회
 * - 상품변경 처리
 * - 상품변경 이력 관리
 */
public interface ProductService {


    /**
     * 고객 정보 조회
     * UFR-PROD-020 구현
     * 
     * @param lineNumber 회선번호
     * @return 고객 정보 응답
     */
    CustomerInfoResponse getCustomerInfo(String lineNumber);

    /**
     * 변경 가능한 상품 목록 조회
     * UFR-PROD-020 구현
     * 
     * @param currentProductCode 현재 상품코드 (필터링용)
     * @return 가용 상품 목록 응답
     */
    AvailableProductsResponse getAvailableProducts(String currentProductCode);

    /**
     * 상품변경 사전체크
     * UFR-PROD-030 구현
     * 
     * @param request 상품변경 검증 요청
     * @return 검증 결과 응답
     */
    ProductChangeValidationResponse validateProductChange(ProductChangeValidationRequest request);

    /**
     * 상품변경 요청 처리
     * UFR-PROD-040 구현
     * 
     * @param request 상품변경 요청
     * @param userId 요청 사용자 ID
     * @return 상품변경 처리 응답 (동기 처리 시)
     */
    ProductChangeResponse requestProductChange(ProductChangeRequest request, String userId);

    /**
     * 상품변경 비동기 요청 처리
     * UFR-PROD-040 구현
     * 
     * @param request 상품변경 요청
     * @param userId 요청 사용자 ID
     * @return 상품변경 비동기 응답 (접수 완료 시)
     */
    ProductChangeAsyncResponse requestProductChangeAsync(ProductChangeRequest request, String userId);


    /**
     * 상품변경 이력 조회
     * UFR-PROD-040 구현 (이력 관리)
     * 
     * @param lineNumber 회선번호 (선택)
     * @param startDate 조회 시작일 (선택)
     * @param endDate 조회 종료일 (선택)
     * @param pageable 페이징 정보
     * @return 상품변경 이력 응답
     */
    ProductChangeHistoryResponse getProductChangeHistory(String lineNumber, String startDate, String endDate, Pageable pageable);
}