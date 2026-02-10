package com.ecommerce.productservice.api.controller;

import com.ecommerce.productservice.api.dto.CategoryRequest;
import com.ecommerce.productservice.api.dto.CategoryResponse;
import com.ecommerce.productservice.domain.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@DisplayName("CategoryController Integration Tests")
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Nested
    @DisplayName("GET /api/v1/categories - Get Category Tree")
    class GetCategoryTreeTests {

        @Test
        @DisplayName("Should return category tree without authentication")
        void shouldReturnCategoryTreeWithoutAuth() throws Exception {
            // Given
            CategoryResponse child = CategoryResponse.builder()
                    .id(2L)
                    .name("Smartphones")
                    .path("/1")
                    .parentId(1L)
                    .children(new ArrayList<>())
                    .build();

            CategoryResponse root = CategoryResponse.builder()
                    .id(1L)
                    .name("Electronics")
                    .path("")
                    .parentId(null)
                    .children(List.of(child))
                    .build();

            when(categoryService.getCategoryTree()).thenReturn(List.of(root));

            // When/Then
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Electronics"))
                    .andExpect(jsonPath("$[0].children[0].name").value("Smartphones"));
        }

        @Test
        @DisplayName("Should return empty list when no categories")
        void shouldReturnEmptyListWhenNoCategories() throws Exception {
            // Given
            when(categoryService.getCategoryTree()).thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/categories - Create Category")
    class CreateCategoryTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create category when admin")
        void shouldCreateCategoryWhenAdmin() throws Exception {
            // Given
            CategoryRequest request = new CategoryRequest();
            request.setName("Electronics");
            request.setParentId(null);

            CategoryResponse response = CategoryResponse.builder()
                    .id(1L)
                    .name("Electronics")
                    .path("")
                    .parentId(null)
                    .build();

            when(categoryService.createCategory(any())).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Electronics"));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Given
            CategoryRequest request = new CategoryRequest();
            request.setName("Electronics");

            // When/Then
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 403 when not admin")
        void shouldReturn403WhenNotAdmin() throws Exception {
            // Given
            CategoryRequest request = new CategoryRequest();
            request.setName("Electronics");

            // When/Then
            mockMvc.perform(post("/api/v1/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/categories/{id} - Delete Category")
    class DeleteCategoryTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete category when admin")
        void shouldDeleteCategoryWhenAdmin() throws Exception {
            // Given
            doNothing().when(categoryService).deleteCategory(1L);

            // When/Then
            mockMvc.perform(delete("/api/v1/categories/1")
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(categoryService).deleteCategory(1L);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 403 when not admin")
        void shouldReturn403WhenNotAdmin() throws Exception {
            // When/Then
            mockMvc.perform(delete("/api/v1/categories/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
