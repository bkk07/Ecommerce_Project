package com.ecommerce.productservice.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuResponse {
    private Long productId;
    private String name;
    private String description;
    private String brand;
    private Long categoryId;

    private SelectedVariant selectedVariant;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectedVariant {
        private String sku;
        private BigDecimal price;
        private Map<String, Object> specs;
        private List<ImageDto> images;
        private Double averageRating;
        private Long ratingCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageDto {
        private String url;
        private boolean isPrimary;
    }
}
