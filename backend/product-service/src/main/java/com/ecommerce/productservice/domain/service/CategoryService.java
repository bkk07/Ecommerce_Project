package com.ecommerce.productservice.domain.service;

import com.ecommerce.productservice.api.dto.CategoryRequest;
import com.ecommerce.productservice.api.dto.CategoryResponse;
import com.ecommerce.productservice.api.exception.ResourceNotFoundException;
import com.ecommerce.productservice.domain.entity.Category;
import com.ecommerce.productservice.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepo;

    // 1. Create Category
    @Transactional
    @CacheEvict(value = "categories", allEntries = true) // Clear tree cache
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());

        if (request.getParentId() != null) {
            Category parent = categoryRepo.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParent(parent);
            // Build materialized path: includes all ancestor IDs including parent
            // Format: "/1/5/10" where 10 is the parent ID
            // This allows easy subtree queries with LIKE '/1/5/%'
            String parentPath = parent.getPath();
            if (parentPath == null || parentPath.isEmpty()) {
                // Parent is root, so path is just the parent's ID
                category.setPath("/" + parent.getId());
            } else {
                // Append parent's ID to parent's path
                category.setPath(parentPath + "/" + parent.getId());
            }
        } else {
            category.setPath(""); // Root has empty path
        }

        Category saved = categoryRepo.save(category);
        return mapToResponse(saved, false); // False = don't load children deep here
    }

    // 2. Get All as Tree (High Performance)
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'tree'") // Cache the whole tree
    public List<CategoryResponse> getCategoryTree() {
        List<Category> allCategories = categoryRepo.findAll();
        return buildTree(allCategories);
    }

    // 3. Delete Category (Safety Checks)
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // Rule: Prevent delete if it has sub-categories
        if (!category.getSubCategories().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with sub-categories. Delete them first.");
        }

        // Rule: Prevent delete if it has products
        if (!category.getProducts().isEmpty()) {
            throw new IllegalStateException("Cannot delete category containing products. Move products first.");
        }

        categoryRepo.delete(category);
    }

    // --- Helper: Build Tree in O(N) ---
    private List<CategoryResponse> buildTree(List<Category> categories) {
        Map<Long, CategoryResponse> dtoMap = new HashMap<>();
        List<CategoryResponse> roots = new ArrayList<>();

        // 1. Convert all Entities to DTOs
        for (Category cat : categories) {
            CategoryResponse dto = CategoryResponse.builder()
                    .id(cat.getId())
                    .name(cat.getName())
                    .path(cat.getPath())
                    .parentId(cat.getParent() != null ? cat.getParent().getId() : null)
                    .children(new ArrayList<>())
                    .build();
            dtoMap.put(cat.getId(), dto);
        }

        // 2. Assemble Parent-Child relationships
        for (Category cat : categories) {
            CategoryResponse dto = dtoMap.get(cat.getId());
            if (cat.getParent() == null) {
                roots.add(dto);
            } else {
                CategoryResponse parentDto = dtoMap.get(cat.getParent().getId());
                if (parentDto != null) {
                    parentDto.getChildren().add(dto);
                }
            }
        }
        return roots;
    }

    private CategoryResponse mapToResponse(Category c, boolean includeChildren) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .path(c.getPath())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .children(includeChildren ? new ArrayList<>() : null) // Simplified for single creates
                .build();
    }
}