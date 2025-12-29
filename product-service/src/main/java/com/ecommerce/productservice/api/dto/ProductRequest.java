package com.ecommerce.productservice.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ProductRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private String brand;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotEmpty(message = "At least one variant required")
    @Valid
    private List<VariantDto> variants;

    private List<ImageDto> images;
    @Data
    public static class VariantDto {
        @NotBlank private String sku;
        @Positive private BigDecimal price;
        private Map<String, Object> specs;
    }
    @Data
    public static class ImageDto {
        @NotBlank private String url;
        private String publicId;
        private boolean isPrimary;
    }
}