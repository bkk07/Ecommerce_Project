package com.ecommerce.productservice.api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String path; // "Electronics / Laptops / Gaming"
    private Long parentId;

    // 🚀 Recursive List for the "Tree" View
    // This matches your requirement: "Get categories as a nested tree"
    private List<CategoryResponse> children;
}
