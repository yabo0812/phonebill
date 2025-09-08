package com.unicorn.phonebill.product.service;

import com.unicorn.phonebill.product.dto.ProductChangeValidationRequest;
import com.unicorn.phonebill.product.dto.ProductChangeValidationResponse;
import com.unicorn.phonebill.product.dto.ProductInfoDto;
import com.unicorn.phonebill.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 상품변경 검증 서비스
 * 
 * 주요 기능:
 * - 상품변경 사전체크 로직
 * - 판매중인 상품 확인
 * - 사업자 일치 확인  
 * - 회선 사용상태 확인
 * - 검증 결과 상세 정보 제공
 */
@Service
public class ProductValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ProductValidationService.class);

    private final ProductRepository productRepository;
    private final ProductCacheService productCacheService;

    public ProductValidationService(ProductRepository productRepository,
                                  ProductCacheService productCacheService) {
        this.productRepository = productRepository;
        this.productCacheService = productCacheService;
    }

    /**
     * 상품변경 사전체크 실행
     * 
     * @param request 상품변경 검증 요청
     * @return 검증 결과
     */
    public ProductChangeValidationResponse validateProductChange(ProductChangeValidationRequest request) {
        logger.info("상품변경 사전체크 시작: lineNumber={}, current={}, target={}", 
                   request.getLineNumber(), request.getCurrentProductCode(), request.getTargetProductCode());

        List<ProductChangeValidationResponse.ValidationData.ValidationDetail> validationDetails = new ArrayList<>();
        boolean overallSuccess = true;
        StringBuilder failureReasonBuilder = new StringBuilder();

        try {
            // 1. 대상 상품 판매 여부 확인
            boolean isProductAvailable = validateProductAvailability(request.getTargetProductCode(), validationDetails);
            if (!isProductAvailable) {
                overallSuccess = false;
                failureReasonBuilder.append("변경 대상 상품이 판매중이 아닙니다. ");
            }

            // 2. 사업자 일치 확인
            boolean isOperatorMatch = validateOperatorMatch(request.getCurrentProductCode(), 
                                                          request.getTargetProductCode(), validationDetails);
            if (!isOperatorMatch) {
                overallSuccess = false;
                failureReasonBuilder.append("현재 상품과 변경 대상 상품의 사업자가 일치하지 않습니다. ");
            }

            // 3. 회선 상태 확인
            boolean isLineStatusValid = validateLineStatus(request.getLineNumber(), validationDetails);
            if (!isLineStatusValid) {
                overallSuccess = false;
                failureReasonBuilder.append("회선 상태가 상품변경이 불가능한 상태입니다. ");
            }

            // 검증 결과 생성
            ProductChangeValidationResponse.ValidationData validationData = 
                ProductChangeValidationResponse.ValidationData.builder()
                    .validationResult(overallSuccess ? 
                        ProductChangeValidationResponse.ValidationResult.SUCCESS : 
                        ProductChangeValidationResponse.ValidationResult.FAILURE)
                    .validationDetails(validationDetails)
                    .failureReason(overallSuccess ? null : failureReasonBuilder.toString().trim())
                    .build();

            logger.info("상품변경 사전체크 완료: lineNumber={}, result={}", 
                       request.getLineNumber(), overallSuccess ? "SUCCESS" : "FAILURE");

            return ProductChangeValidationResponse.success(validationData);

        } catch (Exception e) {
            logger.error("상품변경 사전체크 중 오류 발생: lineNumber={}", request.getLineNumber(), e);
            
            // 오류 발생 시 실패 처리
            List<ProductChangeValidationResponse.ValidationData.ValidationDetail> errorDetails = new ArrayList<>();
            errorDetails.add(ProductChangeValidationResponse.ValidationData.ValidationDetail.builder()
                .checkType(ProductChangeValidationResponse.CheckType.PRODUCT_AVAILABLE)
                .result(ProductChangeValidationResponse.CheckResult.FAIL)
                .message("검증 중 시스템 오류가 발생했습니다")
                .build());
            
            return ProductChangeValidationResponse.failure("시스템 오류로 인해 사전체크를 완료할 수 없습니다", errorDetails);
        }
    }

    /**
     * 상품 판매 가능 여부 검증
     */
    private boolean validateProductAvailability(String targetProductCode, 
                                              List<ProductChangeValidationResponse.ValidationData.ValidationDetail> details) {
        logger.debug("상품 판매 가능 여부 검증: {}", targetProductCode);

        try {
            // 1. 캐시에서 상품 상태 조회
            String cachedStatus = productCacheService.getProductStatus(targetProductCode);
            if (StringUtils.hasText(cachedStatus)) {
                boolean isAvailable = "AVAILABLE".equals(cachedStatus);
                addValidationDetail(details, ProductChangeValidationResponse.CheckType.PRODUCT_AVAILABLE,
                                  isAvailable, isAvailable ? "판매중인 상품입니다" : "판매 중단된 상품입니다");
                return isAvailable;
            }

            // 2. 캐시 미스 시 Repository에서 조회
            Optional<com.unicorn.phonebill.product.domain.Product> productOpt = productRepository.findByProductCode(targetProductCode);
            if (!productOpt.isPresent()) {
                addValidationDetail(details, ProductChangeValidationResponse.CheckType.PRODUCT_AVAILABLE,
                                  false, "존재하지 않는 상품코드입니다");
                return false;
            }

            com.unicorn.phonebill.product.domain.Product product = productOpt.get();
            boolean isAvailable = product.isActive();
            String message = isAvailable ? "판매중인 상품입니다" : "판매 중단된 상품입니다";
            
            // 캐시에 저장
            productCacheService.cacheProductStatus(targetProductCode, isAvailable ? "AVAILABLE" : "UNAVAILABLE");
            
            addValidationDetail(details, ProductChangeValidationResponse.CheckType.PRODUCT_AVAILABLE,
                              isAvailable, message);
            return isAvailable;

        } catch (Exception e) {
            logger.error("상품 판매 가능 여부 검증 중 오류: {}", targetProductCode, e);
            addValidationDetail(details, ProductChangeValidationResponse.CheckType.PRODUCT_AVAILABLE,
                              false, "상품 정보 조회 중 오류가 발생했습니다");
            return false;
        }
    }

    /**
     * 사업자 일치 여부 검증
     */
    private boolean validateOperatorMatch(String currentProductCode, String targetProductCode,
                                        List<ProductChangeValidationResponse.ValidationData.ValidationDetail> details) {
        logger.debug("사업자 일치 여부 검증: current={}, target={}", currentProductCode, targetProductCode);

        try {
            // 현재 상품 정보 조회
            ProductInfoDto currentProduct = getCurrentProductInfo(currentProductCode);
            if (currentProduct == null) {
                addValidationDetail(details, ProductChangeValidationResponse.CheckType.OPERATOR_MATCH,
                                  false, "현재 상품 정보를 찾을 수 없습니다");
                return false;
            }

            // 대상 상품 정보 조회
            ProductInfoDto targetProduct = getCurrentProductInfo(targetProductCode);
            if (targetProduct == null) {
                addValidationDetail(details, ProductChangeValidationResponse.CheckType.OPERATOR_MATCH,
                                  false, "변경 대상 상품 정보를 찾을 수 없습니다");
                return false;
            }

            // 사업자 코드 일치 확인
            String currentOperator = currentProduct.getOperatorCode();
            String targetOperator = targetProduct.getOperatorCode();
            
            boolean isMatch = StringUtils.hasText(currentOperator) && currentOperator.equals(targetOperator);
            String message = isMatch ? "사업자가 일치합니다" : 
                           String.format("사업자가 일치하지 않습니다 (현재: %s, 변경: %s)", currentOperator, targetOperator);
            
            addValidationDetail(details, ProductChangeValidationResponse.CheckType.OPERATOR_MATCH, isMatch, message);
            return isMatch;

        } catch (Exception e) {
            logger.error("사업자 일치 여부 검증 중 오류: current={}, target={}", currentProductCode, targetProductCode, e);
            addValidationDetail(details, ProductChangeValidationResponse.CheckType.OPERATOR_MATCH,
                              false, "사업자 정보 조회 중 오류가 발생했습니다");
            return false;
        }
    }

    /**
     * 회선 상태 검증
     */
    private boolean validateLineStatus(String lineNumber,
                                     List<ProductChangeValidationResponse.ValidationData.ValidationDetail> details) {
        logger.debug("회선 상태 검증: {}", lineNumber);

        try {
            // 1. 캐시에서 회선 상태 조회
            String cachedStatus = productCacheService.getLineStatus(lineNumber);
            if (StringUtils.hasText(cachedStatus)) {
                boolean isValid = isValidLineStatus(cachedStatus);
                String message = getLineStatusMessage(cachedStatus);
                addValidationDetail(details, ProductChangeValidationResponse.CheckType.LINE_STATUS, isValid, message);
                return isValid;
            }

            // 2. 캐시 미스 시 실제 조회 (여기서는 임시 로직, 실제로는 KOS 연동)
            String lineStatus = getLineStatusFromRepository(lineNumber);
            if (!StringUtils.hasText(lineStatus)) {
                addValidationDetail(details, ProductChangeValidationResponse.CheckType.LINE_STATUS,
                                  false, "회선 정보를 찾을 수 없습니다");
                return false;
            }

            // 캐시에 저장
            productCacheService.cacheLineStatus(lineNumber, lineStatus);

            boolean isValid = isValidLineStatus(lineStatus);
            String message = getLineStatusMessage(lineStatus);
            
            addValidationDetail(details, ProductChangeValidationResponse.CheckType.LINE_STATUS, isValid, message);
            return isValid;

        } catch (Exception e) {
            logger.error("회선 상태 검증 중 오류: {}", lineNumber, e);
            addValidationDetail(details, ProductChangeValidationResponse.CheckType.LINE_STATUS,
                              false, "회선 상태 조회 중 오류가 발생했습니다");
            return false;
        }
    }

    /**
     * 상품 정보 조회 (캐시 우선)
     */
    private ProductInfoDto getCurrentProductInfo(String productCode) {
        // 캐시에서 먼저 조회
        ProductInfoDto cachedProduct = productCacheService.getCurrentProductInfo(productCode);
        if (cachedProduct != null) {
            return cachedProduct;
        }

        // 캐시 미스 시 Repository에서 조회
        Optional<com.unicorn.phonebill.product.domain.Product> productOpt = productRepository.findByProductCode(productCode);
        if (productOpt.isPresent()) {
            com.unicorn.phonebill.product.domain.Product domainProduct = productOpt.get();
            ProductInfoDto product = ProductInfoDto.builder()
                    .productCode(domainProduct.getProductCode())
                    .productName(domainProduct.getProductName())
                    .monthlyFee(domainProduct.getMonthlyFee())
                    .dataAllowance(domainProduct.getDataAllowance())
                    .voiceAllowance(domainProduct.getVoiceAllowance())
                    .smsAllowance(domainProduct.getSmsAllowance())
                    .operatorCode(domainProduct.getOperatorCode())
                    .description(domainProduct.getDescription())
                    .isAvailable(domainProduct.isActive())
                    .build();
            productCacheService.cacheCurrentProductInfo(productCode, product);
            return product;
        }
        
        return null;
    }

    /**
     * 회선 상태 조회 (실제로는 KOS 연동 필요)
     */
    private String getLineStatusFromRepository(String lineNumber) {
        // TODO: 실제 구현 시 KOS 시스템 연동 또는 DB 조회
        // 현재는 임시 로직
        return "ACTIVE"; // 임시 반환값
    }

    /**
     * 회선 상태 유효성 확인
     */
    private boolean isValidLineStatus(String status) {
        return "ACTIVE".equals(status);
    }

    /**
     * 회선 상태 메시지 생성
     */
    private String getLineStatusMessage(String status) {
        switch (status) {
            case "ACTIVE":
                return "회선이 정상 상태입니다";
            case "SUSPENDED":
                return "회선이 정지 상태입니다";
            case "TERMINATED":
                return "회선이 해지된 상태입니다";
            default:
                return "알 수 없는 회선 상태입니다: " + status;
        }
    }

    /**
     * 검증 상세 정보 추가
     */
    private void addValidationDetail(List<ProductChangeValidationResponse.ValidationData.ValidationDetail> details,
                                   ProductChangeValidationResponse.CheckType checkType, boolean success, String message) {
        details.add(ProductChangeValidationResponse.ValidationData.ValidationDetail.builder()
            .checkType(checkType)
            .result(success ? ProductChangeValidationResponse.CheckResult.PASS : ProductChangeValidationResponse.CheckResult.FAIL)
            .message(message)
            .build());
    }
}