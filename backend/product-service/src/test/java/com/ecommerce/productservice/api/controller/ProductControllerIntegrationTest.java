package com.ecommerce.productservice.api.controller;

import com.ecommerce.productservice.api.dto.ProductRequest;
import com.ecommerce.productservice.api.dto.ProductResponse;
import com.ecommerce.productservice.api.dto.ProductSkuResponse;
import com.ecommerce.productservice.domain.service.ProductService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@DisplayName("ProductController Integration Tests")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Nested
    @DisplayName("GET /api/v1/products/{id} - Get Product By ID")
    class GetProductByIdTests {

        @Test
        @DisplayName("Should return product without authentication")
        void shouldReturnProductWithoutAuth() throws Exception {
            // Given
            ProductResponse response = createProductResponse();
            when(productService.getProductById(1L)).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Test Product"))
                    .andExpect(jsonPath("$.brand").value("Test Brand"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/sku/{sku} - Get Product By SKU")
    class GetProductBySkuTests {

        @Test
        @DisplayName("Should return product by SKU without authentication")
        void shouldReturnProductBySkuWithoutAuth() throws Exception {
            // Given
            ProductSkuResponse response = ProductSkuResponse.builder()
                    .productId(1L)
                    .name("Test Product")
                    .description("Test Description")
                    .brand("Test Brand")
                    .categoryId(1L)
                    .selectedVariant(ProductSkuResponse.SelectedVariant.builder()
                            .sku("SKU-001")
                            .price(new BigDecimal("999.99"))
                            .averageRating(4.5)
                            .ratingCount(100)
                            .build())
                    .build();

            when(productService.getProductBySku("SKU-001")).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/products/sku/SKU-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value(1))
                    .andExpect(jsonPath("$.selectedVariant.sku").value("SKU-001"))
                    .andExpect(jsonPath("$.selectedVariant.price").value(999.99));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/products - Create Product")
    class CreateProductTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create product when admin")
        void shouldCreateProductWhenAdmin() throws Exception {
            // Given
            ProductRequest request = createProductRequest();
            ProductResponse response = createProductResponse();

            when(productService.createProduct(any())).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/products")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Test Product"));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Given
            ProductRequest request = createProductRequest();

            // When/Then
            mockMvc.perform(post("/api/v1/products")
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
            ProductRequest request = createProductRequest();

            // When/Then
            mockMvc.perform(post("/api/v1/products")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // Helper methods
    private ProductRequest createProductRequest() {
        ProductRequest.ImageDto imageDto = new ProductRequest.ImageDto();
        imageDto.setUrl("https://example.com/image.jpg");
        imageDto.setPublicId("img-001");
        imageDto.setPrimary(true);

        ProductRequest.VariantDto variantDto = new ProductRequest.VariantDto();
        variantDto.setSku("SKU-001");
        variantDto.setPrice(new BigDecimal("999.99"));
        variantDto.setSpecs("{\"color\": \"black\"}");
        variantDto.setImages(List.of(imageDto));

        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setDescription("Test Description");
        request.setBrand("Test Brand");
        request.setCategoryId(1L);
        request.setVariants(List.of(variantDto));

        return request;
    }

    private ProductResponse createProductResponse() {
        ProductResponse.ImageResponse imageResponse = ProductResponse.ImageResponse.builder()
                .id(1L)
                .url("https://example.com/image.jpg")
                .publicId("img-001")
                .isPrimary(true)
                .sortOrder(0)
                .build();

        ProductResponse.VariantResponse variantResponse = ProductResponse.VariantResponse.builder()
                .id(1L)
                .sku("SKU-001")
                .price(new BigDecimal("999.99"))
                .specs("{\"color\": \"black\"}")
                .images(List.of(imageResponse))
                .build();

        return ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .brand("Test Brand")
                .categoryId(1L)
                .categoryName("Electronics")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .variants(List.of(variantResponse))
                .build();
    }
}
