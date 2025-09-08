package com.unicorn.phonebill.product.service;

import com.unicorn.phonebill.product.dto.*;
import com.unicorn.phonebill.product.domain.Product;
import com.unicorn.phonebill.product.domain.ProductChangeHistory;
import com.unicorn.phonebill.product.repository.ProductRepository;
import com.unicorn.phonebill.product.repository.ProductChangeHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 상품 관리 서비스 구현체
 * 
 * 주요 기능:
 * - 상품변경 전체 프로세스 관리
 * - KOS 시스템 연동 조율
 * - 캐시 전략 적용
 * - 트랜잭션 관리
 */
@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final ProductChangeHistoryRepository historyRepository;
    private final ProductValidationService validationService;
    private final ProductCacheService cacheService;
    // TODO: KOS 연동 서비스 추가 예정
    // private final KosClientService kosClientService;

    public ProductServiceImpl(ProductRepository productRepository,
                            ProductChangeHistoryRepository historyRepository,
                            ProductValidationService validationService,
                            ProductCacheService cacheService) {
        this.productRepository = productRepository;
        this.historyRepository = historyRepository;
        this.validationService = validationService;
        this.cacheService = cacheService;
    }

    @Override
    public ProductMenuResponse getProductMenu(String userId) {
        logger.info("상품변경 메뉴 조회: userId={}", userId);

        try {
            // 캐시에서 메뉴 정보 조회
            Object cachedMenu = cacheService.getMenuInfo(userId);
            if (cachedMenu instanceof ProductMenuResponse) {
                logger.debug("메뉴 정보 캐시 히트: userId={}", userId);
                return (ProductMenuResponse) cachedMenu;
            }

            // 메뉴 정보 생성 (실제로는 사용자 권한에 따라 동적 생성)
            ProductMenuResponse.MenuData menuData = createMenuData(userId);
            ProductMenuResponse response = ProductMenuResponse.builder()
                    .success(true)
                    .data(menuData)
                    .build();

            // 캐시에 저장
            cacheService.cacheMenuInfo(userId, response);

            logger.info("상품변경 메뉴 조회 완료: userId={}", userId);
            return response;

        } catch (Exception e) {
            logger.error("상품변경 메뉴 조회 중 오류: userId={}", userId, e);
            throw new RuntimeException("메뉴 조회 중 오류가 발생했습니다", e);
        }
    }

    @Override
    public CustomerInfoResponse getCustomerInfo(String lineNumber) {
        logger.info("고객 정보 조회: lineNumber={}", lineNumber);

        try {
            // 캐시에서 고객 정보 조회
            CustomerInfoResponse.CustomerInfo cachedCustomerInfo = cacheService.getCustomerProductInfo(lineNumber);
            if (cachedCustomerInfo != null) {
                logger.debug("고객 정보 캐시 히트: lineNumber={}", lineNumber);
                return CustomerInfoResponse.success(cachedCustomerInfo);
            }

            // 캐시 미스 시 실제 조회 (TODO: KOS 연동)
            CustomerInfoResponse.CustomerInfo customerInfo = getCustomerInfoFromDataSource(lineNumber);
            if (customerInfo == null) {
                throw new RuntimeException("고객 정보를 찾을 수 없습니다: " + lineNumber);
            }

            // 캐시에 저장
            cacheService.cacheCustomerProductInfo(lineNumber, customerInfo);

            logger.info("고객 정보 조회 완료: lineNumber={}, customerId={}", 
                       lineNumber, customerInfo.getCustomerId());
            return CustomerInfoResponse.success(customerInfo);

        } catch (Exception e) {
            logger.error("고객 정보 조회 중 오류: lineNumber={}", lineNumber, e);
            throw new RuntimeException("고객 정보 조회 중 오류가 발생했습니다", e);
        }
    }

    @Override
    public AvailableProductsResponse getAvailableProducts(String currentProductCode, String operatorCode) {
        logger.info("가용 상품 목록 조회: currentProductCode={}, operatorCode={}", currentProductCode, operatorCode);

        try {
            // 캐시에서 상품 목록 조회
            List<ProductInfoDto> cachedProducts = cacheService.getAvailableProducts(operatorCode);
            if (cachedProducts != null && !cachedProducts.isEmpty()) {
                logger.debug("상품 목록 캐시 히트: operatorCode={}, count={}", operatorCode, cachedProducts.size());
                List<ProductInfoDto> filteredProducts = filterProductsByCurrentProduct(cachedProducts, currentProductCode);
                return AvailableProductsResponse.success(filteredProducts);
            }

            // 캐시 미스 시 실제 조회
            List<Product> products = productRepository.findAvailableProductsByOperator(operatorCode);
            List<ProductInfoDto> productDtos = products.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            // 캐시에 저장
            cacheService.cacheAvailableProducts(operatorCode, productDtos);

            // 현재 상품 기준 필터링
            List<ProductInfoDto> filteredProducts = filterProductsByCurrentProduct(productDtos, currentProductCode);

            logger.info("가용 상품 목록 조회 완료: operatorCode={}, totalCount={}, filteredCount={}", 
                       operatorCode, productDtos.size(), filteredProducts.size());
            return AvailableProductsResponse.success(filteredProducts);

        } catch (Exception e) {
            logger.error("가용 상품 목록 조회 중 오류: operatorCode={}", operatorCode, e);
            throw new RuntimeException("상품 목록 조회 중 오류가 발생했습니다", e);
        }
    }

    @Override
    public ProductChangeValidationResponse validateProductChange(ProductChangeValidationRequest request) {
        logger.info("상품변경 사전체크: lineNumber={}, current={}, target={}", 
                   request.getLineNumber(), request.getCurrentProductCode(), request.getTargetProductCode());

        return validationService.validateProductChange(request);
    }

    @Override
    @Transactional
    public ProductChangeResponse requestProductChange(ProductChangeRequest request, String userId) {
        logger.info("상품변경 동기 처리 요청: lineNumber={}, current={}, target={}, userId={}", 
                   request.getLineNumber(), request.getCurrentProductCode(), 
                   request.getTargetProductCode(), userId);

        String requestId = UUID.randomUUID().toString();

        try {
            // 1. 사전체크 재실행
            ProductChangeValidationRequest validationRequest = ProductChangeValidationRequest.builder()
                    .lineNumber(request.getLineNumber())
                    .currentProductCode(request.getCurrentProductCode())
                    .targetProductCode(request.getTargetProductCode())
                    .build();
            
            ProductChangeValidationResponse validationResponse = validationService.validateProductChange(validationRequest);
            if (validationResponse.getData().getValidationResult() == ProductChangeValidationResponse.ValidationResult.FAILURE) {
                throw new RuntimeException("사전체크 실패: " + validationResponse.getData().getFailureReason());
            }

            // 2. 이력 저장 (진행중 상태)
            ProductChangeHistory history = createProductChangeHistory(requestId, request, userId);
            history.markAsProcessing();
            historyRepository.save(history);

            // 3. KOS 연동 처리 (TODO: 실제 KOS 연동 구현)
            ProductChangeResult changeResult = processProductChangeWithKos(request, requestId);

            // 4. 처리 결과에 따른 이력 업데이트
            if (changeResult.isSuccess()) {
                // KOS 응답 데이터를 Map으로 변환
                Map<String, Object> kosResponseData = Map.of(
                    "resultCode", changeResult.getResultCode(),
                    "resultMessage", changeResult.getResultMessage(),
                    "processedAt", LocalDateTime.now().toString()
                );
                history = history.markAsCompleted(changeResult.getResultMessage(), kosResponseData);
                
                // 캐시 무효화
                cacheService.evictProductChangeCaches(
                    request.getLineNumber(), 
                    userId, // customerId 대신 사용
                    request.getCurrentProductCode(),
                    request.getTargetProductCode()
                );
            } else {
                history = history.markAsFailed(changeResult.getResultCode(), changeResult.getFailureReason());
            }
            
            historyRepository.save(history);

            // 5. 응답 생성
            if (changeResult.isSuccess()) {
                ProductInfoDto changedProduct = getProductInfo(request.getTargetProductCode());
                logger.info("상품변경 동기 처리 완료: requestId={}, result=SUCCESS", requestId);
                return ProductChangeResponse.success(requestId, changeResult.getResultCode(), 
                                                   changeResult.getResultMessage(), changedProduct);
            } else {
                logger.error("상품변경 동기 처리 실패: requestId={}, reason={}", requestId, changeResult.getFailureReason());
                throw new RuntimeException("상품변경 처리 실패: " + changeResult.getFailureReason());
            }

        } catch (Exception e) {
            logger.error("상품변경 동기 처리 중 오류: requestId={}", requestId, e);
            
            // 실패 이력 저장
            try {
                Optional<ProductChangeHistory> historyOpt = historyRepository.findByRequestId(requestId);
                if (historyOpt.isPresent()) {
                    ProductChangeHistory history = historyOpt.get();
                    history = history.markAsFailed("SYSTEM_ERROR", e.getMessage());
                    historyRepository.save(history);
                }
            } catch (Exception historyError) {
                logger.error("실패 이력 저장 중 오류: requestId={}", requestId, historyError);
            }
            
            throw new RuntimeException("상품변경 처리 중 오류가 발생했습니다", e);
        }
    }

    @Override
    @Transactional
    public ProductChangeAsyncResponse requestProductChangeAsync(ProductChangeRequest request, String userId) {
        logger.info("상품변경 비동기 처리 요청: lineNumber={}, current={}, target={}, userId={}", 
                   request.getLineNumber(), request.getCurrentProductCode(), 
                   request.getTargetProductCode(), userId);

        String requestId = UUID.randomUUID().toString();

        try {
            // 1. 사전체크 재실행
            ProductChangeValidationRequest validationRequest = ProductChangeValidationRequest.builder()
                    .lineNumber(request.getLineNumber())
                    .currentProductCode(request.getCurrentProductCode())
                    .targetProductCode(request.getTargetProductCode())
                    .build();
            
            ProductChangeValidationResponse validationResponse = validationService.validateProductChange(validationRequest);
            if (validationResponse.getData().getValidationResult() == ProductChangeValidationResponse.ValidationResult.FAILURE) {
                throw new RuntimeException("사전체크 실패: " + validationResponse.getData().getFailureReason());
            }

            // 2. 이력 저장 (접수 대기 상태)
            ProductChangeHistory history = createProductChangeHistory(requestId, request, userId);
            historyRepository.save(history);

            // 3. 비동기 처리 큐에 등록 (TODO: 메시지 큐 연동)
            // messageQueueService.sendProductChangeRequest(request, requestId, userId);

            logger.info("상품변경 비동기 처리 접수 완료: requestId={}", requestId);
            return ProductChangeAsyncResponse.accepted(requestId, "상품 변경 요청이 접수되었습니다");

        } catch (Exception e) {
            logger.error("상품변경 비동기 처리 접수 중 오류: requestId={}", requestId, e);
            throw new RuntimeException("상품변경 요청 접수 중 오류가 발생했습니다", e);
        }
    }

    @Override
    public ProductChangeResultResponse getProductChangeResult(String requestId) {
        logger.info("상품변경 결과 조회: requestId={}", requestId);

        try {
            // 캐시에서 결과 조회
            ProductChangeResultResponse.ProductChangeResult cachedResult = cacheService.getProductChangeResult(requestId);
            if (cachedResult != null) {
                logger.debug("상품변경 결과 캐시 히트: requestId={}", requestId);
                return ProductChangeResultResponse.success(cachedResult);
            }

            // 캐시 미스 시 DB에서 조회
            Optional<ProductChangeHistory> historyOpt = historyRepository.findByRequestId(requestId);
            if (!historyOpt.isPresent()) {
                throw new RuntimeException("요청 정보를 찾을 수 없습니다: " + requestId);
            }
            
            ProductChangeHistory history = historyOpt.get();

            ProductChangeResultResponse.ProductChangeResult result = convertToResultDto(history);

            // 완료된 결과만 캐시에 저장
            if (history.getProcessStatus().equals("COMPLETED") || history.getProcessStatus().equals("FAILED")) {
                cacheService.cacheProductChangeResult(requestId, result);
            }

            logger.info("상품변경 결과 조회 완료: requestId={}, status={}", requestId, history.getProcessStatus());
            return ProductChangeResultResponse.success(result);

        } catch (Exception e) {
            logger.error("상품변경 결과 조회 중 오류: requestId={}", requestId, e);
            throw new RuntimeException("상품변경 결과 조회 중 오류가 발생했습니다", e);
        }
    }

    @Override
    public ProductChangeHistoryResponse getProductChangeHistory(String lineNumber, String startDate, String endDate, Pageable pageable) {
        logger.info("상품변경 이력 조회: lineNumber={}, startDate={}, endDate={}, page={}", 
                   lineNumber, startDate, endDate, pageable.getPageNumber());

        try {
            LocalDate start = StringUtils.hasText(startDate) ? LocalDate.parse(startDate) : null;
            LocalDate end = StringUtils.hasText(endDate) ? LocalDate.parse(endDate) : null;

            Page<ProductChangeHistory> historyPage;
            if (start != null && end != null) {
                LocalDateTime startDateTime = start.atStartOfDay();
                LocalDateTime endDateTime = end.atTime(23, 59, 59);
                historyPage = historyRepository.findByLineNumberAndPeriod(lineNumber, startDateTime, endDateTime, pageable);
            } else if (StringUtils.hasText(lineNumber)) {
                historyPage = historyRepository.findByLineNumber(lineNumber, pageable);
            } else {
                // 전체 이력 조회
                historyPage = historyRepository.findByPeriod(
                    start != null ? start.atStartOfDay() : LocalDateTime.now().minusMonths(1),
                    end != null ? end.atTime(23, 59, 59) : LocalDateTime.now(),
                    pageable
                );
            }
            
            List<ProductChangeHistoryResponse.ProductChangeHistoryItem> historyItems = historyPage.getContent().stream()
                    .map(this::convertToHistoryItem)
                    .collect(Collectors.toList());

            ProductChangeHistoryResponse.PaginationInfo paginationInfo = ProductChangeHistoryResponse.PaginationInfo.builder()
                    .page(pageable.getPageNumber() + 1) // 0-based to 1-based
                    .size(pageable.getPageSize())
                    .totalElements(historyPage.getTotalElements())
                    .totalPages(historyPage.getTotalPages())
                    .hasNext(historyPage.hasNext())
                    .hasPrevious(historyPage.hasPrevious())
                    .build();

            logger.info("상품변경 이력 조회 완료: lineNumber={}, totalElements={}", lineNumber, historyPage.getTotalElements());
            return ProductChangeHistoryResponse.success(historyItems, paginationInfo);

        } catch (Exception e) {
            logger.error("상품변경 이력 조회 중 오류: lineNumber={}", lineNumber, e);
            throw new RuntimeException("상품변경 이력 조회 중 오류가 발생했습니다", e);
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * 메뉴 데이터 생성
     */
    private ProductMenuResponse.MenuData createMenuData(String userId) {
        // TODO: 실제로는 사용자 권한 및 고객 정보에 따라 동적 생성
        return ProductMenuResponse.MenuData.builder()
                .customerId("CUST001") // 임시값
                .lineNumber("01012345678") // 임시값
                .menuItems(Arrays.asList(
                    ProductMenuResponse.MenuItem.builder()
                        .menuId("MENU001")
                        .menuName("상품변경")
                        .available(true)
                        .description("현재 이용 중인 상품을 다른 상품으로 변경합니다")
                        .build()
                ))
                .build();
    }

    /**
     * 데이터소스에서 고객 정보 조회
     */
    private CustomerInfoResponse.CustomerInfo getCustomerInfoFromDataSource(String lineNumber) {
        // TODO: 실제 KOS 연동 또는 DB 조회 구현
        // 현재는 임시 데이터 반환
        ProductInfoDto currentProduct = ProductInfoDto.builder()
                .productCode("PLAN001")
                .productName("5G 베이직 플랜")
                .monthlyFee(new java.math.BigDecimal("45000"))
                .dataAllowance("50GB")
                .voiceAllowance("무제한")
                .smsAllowance("기본 무료")
                .isAvailable(true)
                .operatorCode("MVNO001")
                .build();

        return CustomerInfoResponse.CustomerInfo.builder()
                .customerId("CUST001")
                .lineNumber(lineNumber)
                .customerName("홍길동")
                .currentProduct(currentProduct)
                .lineStatus("ACTIVE")
                .build();
    }

    /**
     * 현재 상품 기준 필터링
     */
    private List<ProductInfoDto> filterProductsByCurrentProduct(List<ProductInfoDto> products, String currentProductCode) {
        if (!StringUtils.hasText(currentProductCode)) {
            return products;
        }
        
        return products.stream()
                .filter(product -> !product.getProductCode().equals(currentProductCode))
                .collect(Collectors.toList());
    }

    /**
     * Domain을 DTO로 변환
     */
    private ProductInfoDto convertToDto(Product product) {
        return ProductInfoDto.builder()
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .monthlyFee(product.getMonthlyFee())
                .dataAllowance(product.getDataAllowance())
                .voiceAllowance(product.getVoiceAllowance())
                .smsAllowance(product.getSmsAllowance())
                .isAvailable(product.canChangeTo(null)) // 변경 가능 여부
                .operatorCode(product.getOperatorCode())
                .build();
    }

    /**
     * 상품변경 이력 객체 생성
     */
    private ProductChangeHistory createProductChangeHistory(String requestId, ProductChangeRequest request, String userId) {
        return ProductChangeHistory.createNew(
                requestId,
                request.getLineNumber(),
                userId, // customerId로 사용
                request.getCurrentProductCode(),
                request.getTargetProductCode()
        );
    }

    /**
     * KOS 연동 상품변경 처리 (임시 구현)
     */
    private ProductChangeResult processProductChangeWithKos(ProductChangeRequest request, String requestId) {
        // TODO: 실제 KOS 연동 구현
        // 현재는 임시 성공 결과 반환
        try {
            Thread.sleep(100); // 처리 시간 시뮬레이션
            return ProductChangeResult.builder()
                    .success(true)
                    .resultCode("SUCCESS")
                    .resultMessage("상품 변경이 완료되었습니다")
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProductChangeResult.builder()
                    .success(false)
                    .resultCode("SYSTEM_ERROR")
                    .failureReason("처리 중 시스템 오류 발생")
                    .build();
        }
    }

    /**
     * 상품 정보 조회
     */
    private ProductInfoDto getProductInfo(String productCode) {
        ProductInfoDto cached = cacheService.getCurrentProductInfo(productCode);
        if (cached != null) {
            return cached;
        }

        Optional<Product> productOpt = productRepository.findByProductCode(productCode);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            ProductInfoDto dto = convertToDto(product);
            cacheService.cacheCurrentProductInfo(productCode, dto);
            return dto;
        }
        
        return null;
    }

    /**
     * ProductChangeHistory를 ProductChangeResult DTO로 변환
     */
    private ProductChangeResultResponse.ProductChangeResult convertToResultDto(ProductChangeHistory history) {
        return ProductChangeResultResponse.ProductChangeResult.builder()
                .requestId(history.getRequestId())
                .lineNumber(history.getLineNumber())
                .processStatus(ProductChangeResultResponse.ProcessStatus.valueOf(history.getProcessStatus().name()))
                .currentProductCode(history.getCurrentProductCode())
                .targetProductCode(history.getTargetProductCode())
                .requestedAt(history.getRequestedAt())
                .processedAt(history.getProcessedAt())
                .resultCode(history.getResultCode())
                .resultMessage(history.getResultMessage())
                .failureReason(history.getFailureReason())
                .build();
    }

    /**
     * ProductChangeHistory를 HistoryItem DTO로 변환
     */
    private ProductChangeHistoryResponse.ProductChangeHistoryItem convertToHistoryItem(ProductChangeHistory history) {
        return ProductChangeHistoryResponse.ProductChangeHistoryItem.builder()
                .requestId(history.getRequestId())
                .lineNumber(history.getLineNumber())
                .processStatus(history.getProcessStatus().name())
                .currentProductCode(history.getCurrentProductCode())
                .currentProductName("현재상품명") // TODO: 상품명 조회 로직 추가
                .targetProductCode(history.getTargetProductCode())
                .targetProductName("변경상품명") // TODO: 상품명 조회 로직 추가
                .requestedAt(history.getRequestedAt())
                .processedAt(history.getProcessedAt())
                .resultMessage(history.getResultMessage())
                .build();
    }

    /**
     * 상품변경 결과 임시 클래스
     */
    private static class ProductChangeResult {
        private final boolean success;
        private final String resultCode;
        private final String resultMessage;
        private final String failureReason;

        private ProductChangeResult(boolean success, String resultCode, String resultMessage, String failureReason) {
            this.success = success;
            this.resultCode = resultCode;
            this.resultMessage = resultMessage;
            this.failureReason = failureReason;
        }

        public static ProductChangeResultBuilder builder() {
            return new ProductChangeResultBuilder();
        }

        public boolean isSuccess() { return success; }
        public String getResultCode() { return resultCode; }
        public String getResultMessage() { return resultMessage; }
        public String getFailureReason() { return failureReason; }

        public static class ProductChangeResultBuilder {
            private boolean success;
            private String resultCode;
            private String resultMessage;
            private String failureReason;

            public ProductChangeResultBuilder success(boolean success) { this.success = success; return this; }
            public ProductChangeResultBuilder resultCode(String resultCode) { this.resultCode = resultCode; return this; }
            public ProductChangeResultBuilder resultMessage(String resultMessage) { this.resultMessage = resultMessage; return this; }
            public ProductChangeResultBuilder failureReason(String failureReason) { this.failureReason = failureReason; return this; }

            public ProductChangeResult build() {
                return new ProductChangeResult(success, resultCode, resultMessage, failureReason);
            }
        }
    }
}