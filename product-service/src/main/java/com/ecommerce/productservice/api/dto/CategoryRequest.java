package com.ecommerce.productservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    private Long parentId; // Optional: Null means it's a "Root" category
}
