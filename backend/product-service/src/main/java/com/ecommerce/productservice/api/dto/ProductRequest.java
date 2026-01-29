package com.ecommerce.productservice.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    // 🔥 Variant DTO
    @Data
    public static class VariantDto {

        @NotBlank(message = "SKU is required")
        private String sku;

        @Positive(message = "Price must be greater than zero")
        private BigDecimal price;

        private Map<String, Object> specs;

        @NotEmpty(message = "At least one image required for variant")
        @Valid
        private List<ImageDto> images;
    }

    // 🔥 Image DTO
    @Data
    public static class ImageDto {

        @NotBlank(message = "Image URL is required")
        private String url;

        private String publicId;

        @JsonProperty("isPrimary")
        private boolean isPrimary;
    }
}
