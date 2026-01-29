package com.ecommerce.productservice.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private String brand;

    // Flattened Category Data (Easier for Frontend Tables)
    private Long categoryId;
    private String categoryName;

    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Nested Lists
    private List<VariantResponse> variants;

    // --- Inner DTOs ---

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantResponse {
        private Long id;
        private String sku;
        private BigDecimal price;
        // Flexible specs map (e.g., "Color": "Red", "Size": "42")
        private Map<String, Object> specs;
        private List<ImageResponse> images;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageResponse {
        private Long id;
        private String url;
        private String publicId; // Useful if frontend needs to request specific transformations
        private boolean isPrimary;
        private int sortOrder;
    }
}