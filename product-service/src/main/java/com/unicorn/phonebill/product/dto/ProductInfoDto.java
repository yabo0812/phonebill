package com.unicorn.phonebill.product.dto;

import com.unicorn.phonebill.product.domain.Product;
import com.unicorn.phonebill.product.domain.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 상품 정보 DTO
 */
@Getter
@Builder
@Schema(description = "상품 정보")
public class ProductInfoDto {

    @Schema(description = "상품 코드", example = "PLAN001")
    private final String productCode;

    @Schema(description = "상품명", example = "5G 프리미엄 플랜")
    private final String productName;

    @Schema(description = "월 요금", example = "55000")
    private final BigDecimal monthlyFee;

    @Schema(description = "데이터 제공량", example = "100GB")
    private final String dataAllowance;

    @Schema(description = "음성 제공량", example = "무제한")
    private final String voiceAllowance;

    @Schema(description = "SMS 제공량", example = "기본 무료")
    private final String smsAllowance;

    @Schema(description = "변경 가능 여부", example = "true")
    private final boolean isAvailable;

    @Schema(description = "사업자 코드", example = "MVNO001")
    private final String operatorCode;

    @Schema(description = "상품 설명")
    private final String description;

    /**
     * 도메인 모델에서 DTO로 변환
     */
    public static ProductInfoDto fromDomain(Product product) {
        if (product == null) {
            return null;
        }

        return ProductInfoDto.builder()
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .monthlyFee(product.getMonthlyFee())
                .dataAllowance(product.getDataAllowance())
                .voiceAllowance(product.getVoiceAllowance())
                .smsAllowance(product.getSmsAllowance())
                .isAvailable(product.getStatus() == ProductStatus.ACTIVE)
                .operatorCode(product.getOperatorCode())
                .description(product.getDescription())
                .build();
    }

    /**
     * DTO에서 도메인 모델로 변환
     */
    public Product toDomain() {
        return Product.builder()
                .productCode(this.productCode)
                .productName(this.productName)
                .monthlyFee(this.monthlyFee)
                .dataAllowance(this.dataAllowance)
                .voiceAllowance(this.voiceAllowance)
                .smsAllowance(this.smsAllowance)
                .status(this.isAvailable ? ProductStatus.ACTIVE : ProductStatus.DISCONTINUED)
                .operatorCode(this.operatorCode)
                .description(this.description)
                .build();
    }
}