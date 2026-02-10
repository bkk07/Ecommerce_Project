package com.ecommerce.productservice.domain.service;

import com.ecommerce.productservice.api.dto.CategoryRequest;
import com.ecommerce.productservice.api.dto.CategoryResponse;
import com.ecommerce.productservice.api.exception.ResourceNotFoundException;
import com.ecommerce.productservice.domain.entity.Category;
import com.ecommerce.productservice.domain.entity.Product;
import com.ecommerce.productservice.domain.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepo;

    @InjectMocks
    private CategoryService categoryService;

    private Category rootCategory;
    private Category childCategory;

    @BeforeEach
    void setUp() {
        rootCategory = new Category();
        rootCategory.setId(1L);
        rootCategory.setName("Electronics");
        rootCategory.setPath("");
        rootCategory.setSubCategories(new ArrayList<>());
        rootCategory.setProducts(new ArrayList<>());

        childCategory = new Category();
        childCategory.setId(2L);
        childCategory.setName("Smartphones");
        childCategory.setPath("/1");
        childCategory.setParent(rootCategory);
        childCategory.setSubCategories(new ArrayList<>());
        childCategory.setProducts(new ArrayList<>());
    }

    @Nested
    @DisplayName("createCategory Tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create root category with empty path")
        void shouldCreateRootCategoryWithEmptyPath() {
            // Given
            CategoryRequest request = new CategoryRequest();
            request.setName("Electronics");
            request.setParentId(null);

            Category savedCategory = new Category();
            savedCategory.setId(1L);
            savedCategory.setName("Electronics");
            savedCategory.setPath("");

            when(categoryRepo.save(any(Category.class))).thenReturn(savedCategory);

            // When
            CategoryResponse response = categoryService.createCategory(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Electronics");
            assertThat(response.getPath()).isEqualTo("");
            assertThat(response.getParentId()).isNull();
        }

        @Test
        @DisplayName("Should create child category with correct path including parent ID")
        void shouldCreateChildCategoryWithCorrectPath() {
            // Given
            CategoryRequest request = new CategoryRequest();
            request.setName("Smartphones");
            request.setParentId(1L);

            when(categoryRepo.findById(1L)).thenReturn(Optional.of(rootCategory));
            
            ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
            when(categoryRepo.save(categoryCaptor.capture())).thenAnswer(invocation -> {
                Category cat = invocation.getArgument(0);
                cat.setId(2L);
                return cat;
            });

            // When
            CategoryResponse response = categoryService.createCategory(request);

            // Then
            Category savedCategory = categoryCaptor.getValue();
            assertThat(savedCategory.getPath()).isEqualTo("/1");
            assertThat(response.getName()).isEqualTo("Smartphones");
        }

        @Test
        @DisplayName("Should create deeply nested category with full ancestor path")
        void shouldCreateDeeplyNestedCategoryWithFullPath() {
            // Given
            Category parentWithPath = new Category();
            parentWithPath.setId(5L);
            parentWithPath.setName("Apple");
            parentWithPath.setPath("/1/2"); // Electronics -> Smartphones -> Apple

            CategoryRequest request = new CategoryRequest();
            request.setName("iPhone 15");
            request.setParentId(5L);

            when(categoryRepo.findById(5L)).thenReturn(Optional.of(parentWithPath));
            
            ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
            when(categoryRepo.save(categoryCaptor.capture())).thenAnswer(invocation -> {
                Category cat = invocation.getArgument(0);
                cat.setId(10L);
                return cat;
            });

            // When
            categoryService.createCategory(request);

            // Then
            Category savedCategory = categoryCaptor.getValue();
            assertThat(savedCategory.getPath()).isEqualTo("/1/2/5");
        }

        @Test
        @DisplayName("Should throw exception when parent category not found")
        void shouldThrowExceptionWhenParentNotFound() {
            // Given
            CategoryRequest request = new CategoryRequest();
            request.setName("Smartphones");
            request.setParentId(99L);

            when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Parent category not found");
        }
    }

    @Nested
    @DisplayName("getCategoryTree Tests")
    class GetCategoryTreeTests {

        @Test
        @DisplayName("Should build category tree correctly")
        void shouldBuildCategoryTreeCorrectly() {
            // Given
            List<Category> allCategories = List.of(rootCategory, childCategory);
            when(categoryRepo.findAll()).thenReturn(allCategories);

            // When
            List<CategoryResponse> tree = categoryService.getCategoryTree();

            // Then
            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).getName()).isEqualTo("Electronics");
            assertThat(tree.get(0).getChildren()).hasSize(1);
            assertThat(tree.get(0).getChildren().get(0).getName()).isEqualTo("Smartphones");
        }

        @Test
        @DisplayName("Should return empty list when no categories exist")
        void shouldReturnEmptyListWhenNoCategories() {
            // Given
            when(categoryRepo.findAll()).thenReturn(List.of());

            // When
            List<CategoryResponse> tree = categoryService.getCategoryTree();

            // Then
            assertThat(tree).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteCategory Tests")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Should delete category successfully when no children or products")
        void shouldDeleteCategorySuccessfully() {
            // Given
            when(categoryRepo.findById(1L)).thenReturn(Optional.of(rootCategory));

            // When
            categoryService.deleteCategory(1L);

            // Then
            verify(categoryRepo).delete(rootCategory);
        }

        @Test
        @DisplayName("Should throw exception when category has sub-categories")
        void shouldThrowExceptionWhenHasSubCategories() {
            // Given
            rootCategory.getSubCategories().add(childCategory);
            when(categoryRepo.findById(1L)).thenReturn(Optional.of(rootCategory));

            // When/Then
            assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("sub-categories");
        }

        @Test
        @DisplayName("Should throw exception when category has products")
        void shouldThrowExceptionWhenHasProducts() {
            // Given
            Product product = new Product();
            product.setId(1L);
            rootCategory.getProducts().add(product);
            when(categoryRepo.findById(1L)).thenReturn(Optional.of(rootCategory));

            // When/Then
            assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("products");
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void shouldThrowExceptionWhenCategoryNotFound() {
            // Given
            when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found");
        }
    }
}
