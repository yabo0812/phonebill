package com.unicorn.phonebill.product.service;

import com.unicorn.phonebill.product.dto.*;
import com.unicorn.phonebill.product.domain.Product;
import com.unicorn.phonebill.product.domain.ProductChangeHistory;
import com.unicorn.phonebill.product.domain.ProductChangeResult;
import com.unicorn.phonebill.product.repository.ProductRepository;
import com.unicorn.phonebill.product.repository.ProductChangeHistoryRepository;
import com.unicorn.phonebill.product.dto.kos.KosCommonResponse;
import com.unicorn.phonebill.product.dto.kos.KosProductInquiryResponse;
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
    private final KosClientService kosClientService;

    public ProductServiceImpl(ProductRepository productRepository,
                            ProductChangeHistoryRepository historyRepository,
                            ProductValidationService validationService,
                            ProductCacheService cacheService,
                            KosClientService kosClientService) {
        this.productRepository = productRepository;
        this.historyRepository = historyRepository;
        this.validationService = validationService;
        this.cacheService = cacheService;
        this.kosClientService = kosClientService;
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
    public AvailableProductsResponse getAvailableProducts(String currentProductCode) {
        logger.info("가용 상품 목록 조회: currentProductCode={}", currentProductCode);

        try {
            // 캐시에서 상품 목록 조회
            List<ProductInfoDto> cachedProducts = cacheService.getAvailableProducts("all");
            if (cachedProducts != null && !cachedProducts.isEmpty()) {
                logger.debug("상품 목록 캐시 히트: count={}", cachedProducts.size());
                List<ProductInfoDto> filteredProducts = filterProductsByCurrentProduct(cachedProducts, currentProductCode);
                return AvailableProductsResponse.success(filteredProducts);
            }

            // 캐시 미스 시 실제 조회
            List<Product> products = productRepository.findAvailableProducts();
            List<ProductInfoDto> productDtos = products.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            // 캐시에 저장
            cacheService.cacheAvailableProducts("all", productDtos);

            // 현재 상품 기준 필터링
            List<ProductInfoDto> filteredProducts = filterProductsByCurrentProduct(productDtos, currentProductCode);

            logger.info("가용 상품 목록 조회 완료: totalCount={}, filteredCount={}", 
                       productDtos.size(), filteredProducts.size());
            return AvailableProductsResponse.success(filteredProducts);

        } catch (Exception e) {
            logger.error("가용 상품 목록 조회 중 오류", e);
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
                // KOS 응답 데이터 사용 (실제 응답 데이터 또는 기본 데이터)
                Map<String, Object> kosResponseData = changeResult.getKosResponseData();
                if (kosResponseData == null) {
                    kosResponseData = Map.of(
                        "resultCode", changeResult.getResultCode(),
                        "resultMessage", changeResult.getResultMessage(),
                        "kosOrderNumber", changeResult.getKosOrderNumber() != null ? changeResult.getKosOrderNumber() : "N/A",
                        "effectiveDate", changeResult.getEffectiveDate() != null ? changeResult.getEffectiveDate() : "N/A",
                        "processedAt", LocalDateTime.now().toString()
                    );
                }
                
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
     * 데이터소스에서 고객 정보 조회 (KOS 연동)
     */
    private CustomerInfoResponse.CustomerInfo getCustomerInfoFromDataSource(String lineNumber) {
        try {
            logger.debug("KOS 시스템에서 고객 정보 조회: lineNumber={}", lineNumber);
            
            // KOS 시스템 호출
            KosCommonResponse<KosProductInquiryResponse> kosResponse = kosClientService.getProductInquiry(lineNumber);
            
            if (kosResponse.getSuccess() && kosResponse.getData() != null) {
                KosProductInquiryResponse kosData = kosResponse.getData();
                
                // KOS 응답을 내부 DTO로 변환
                ProductInfoDto currentProduct = ProductInfoDto.builder()
                        .productCode(kosData.getProductInfo().getCurrentProductCode())
                        .productName(kosData.getProductInfo().getCurrentProductName())
                        .monthlyFee(kosData.getProductInfo().getMonthlyFee())
                        .dataAllowance(kosData.getProductInfo().getDataAllowance())
                        .voiceAllowance(kosData.getProductInfo().getVoiceAllowance())
                        .smsAllowance(kosData.getProductInfo().getSmsAllowance())
                        .isAvailable("ACTIVE".equals(kosData.getProductInfo().getProductStatus()))
                        .operatorCode(kosData.getCustomerInfo().getOperatorCode())
                        .build();

                return CustomerInfoResponse.CustomerInfo.builder()
                        .customerId(kosData.getCustomerInfo().getCustomerId())
                        .lineNumber(lineNumber)
                        .customerName(kosData.getCustomerInfo().getCustomerName())
                        .currentProduct(currentProduct)
                        .lineStatus(kosData.getCustomerInfo().getLineStatus())
                        .build();
            } else {
                logger.error("KOS 시스템에서 고객 정보를 찾을 수 없습니다: lineNumber={}, resultCode={}, resultMessage={}", 
                           lineNumber, kosResponse.getResultCode(), kosResponse.getResultMessage());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("KOS 연동 중 오류 발생: lineNumber={}", lineNumber, e);
            throw new RuntimeException("고객 정보 조회 중 시스템 오류가 발생했습니다", e);
        }
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
                requestId, // UUID는 엔티티에서 자동 생성됨
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
        logger.info("KOS 상품 변경 처리 시작: requestId={}, lineNumber={}", requestId, request.getLineNumber());

        try {
            // KOS 상품변경 API 호출
            Map<String, Object> kosResponse = kosClientService.changeProductInKos(
                request.getLineNumber(), 
                request.getCurrentProductCode(), 
                request.getTargetProductCode()
            );

            // KOS 응답 분석
            Boolean success = (Boolean) kosResponse.get("success");
            String resultCode = (String) kosResponse.get("resultCode");
            String resultMessage = (String) kosResponse.get("resultMessage");
            
            if (Boolean.TRUE.equals(success) && "0000".equals(resultCode)) {
                logger.info("KOS 상품 변경 성공: requestId={}, lineNumber={}", requestId, request.getLineNumber());
                
                // data 섹션에서 상세 정보 추출
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) kosResponse.get("data");
                if (data != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> changeInfo = (Map<String, Object>) data.get("changeInfo");
                    if (changeInfo != null) {
                        String kosOrderNumber = (String) changeInfo.get("kosOrderNumber");
                        String effectiveDate = (String) changeInfo.get("effectiveDate");
                        
                        return ProductChangeResult.builder()
                                .success(true)
                                .resultCode(resultCode)
                                .resultMessage(resultMessage)
                                .kosOrderNumber(kosOrderNumber)
                                .effectiveDate(effectiveDate)
                                .kosResponseData(kosResponse)
                                .build();
                    }
                }
                
                return ProductChangeResult.builder()
                        .success(true)
                        .resultCode(resultCode)
                        .resultMessage(resultMessage)
                        .kosResponseData(kosResponse)
                        .build();
                        
            } else {
                logger.error("KOS 상품 변경 실패: requestId={}, resultCode={}, resultMessage={}", 
                           requestId, resultCode, resultMessage);
                           
                return ProductChangeResult.builder()
                        .success(false)
                        .resultCode(resultCode != null ? resultCode : "KOS_ERROR")
                        .failureReason(resultMessage != null ? resultMessage : "KOS 시스템 오류")
                        .kosResponseData(kosResponse)
                        .build();
            }

        } catch (Exception e) {
            logger.error("KOS 연동 중 예외 발생: requestId={}, lineNumber={}", requestId, request.getLineNumber(), e);
            
            return ProductChangeResult.builder()
                    .success(false)
                    .resultCode("SYSTEM_ERROR")
                    .failureReason("KOS 시스템 연동 중 오류가 발생했습니다: " + e.getMessage())
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

}