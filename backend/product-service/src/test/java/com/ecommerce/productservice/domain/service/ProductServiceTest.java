package com.ecommerce.productservice.domain.service;

import com.ecommerce.productservice.api.dto.ProductRequest;
import com.ecommerce.productservice.api.dto.ProductResponse;
import com.ecommerce.productservice.api.dto.ProductSkuResponse;
import com.ecommerce.productservice.api.exception.ResourceNotFoundException;
import com.ecommerce.productservice.domain.entity.*;
import com.ecommerce.productservice.domain.repository.CategoryRepository;
import com.ecommerce.productservice.domain.repository.OutboxRepository;
import com.ecommerce.productservice.domain.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepo;

    @Mock
    private CategoryRepository categoryRepo;

    @Mock
    private OutboxRepository outboxRepo;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProductService productService;

    private Category testCategory;
    private Product testProduct;
    private ProductVariant testVariant;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Electronics");
        testCategory.setPath("");

        testVariant = new ProductVariant();
        testVariant.setId(1L);
        testVariant.setSku("SKU-001");
        testVariant.setPrice(new BigDecimal("999.99"));
        testVariant.setSpecs("{\"color\": \"black\"}");
        testVariant.setAverageRating(4.5);
        testVariant.setRatingCount(100);

        ProductImage image = new ProductImage();
        image.setId(1L);
        image.setUrl("https://example.com/image.jpg");
        image.setPublicId("img-001");
        image.setPrimary(true);
        testVariant.setImages(new ArrayList<>(List.of(image)));

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setBrand("Test Brand");
        testProduct.setCategory(testCategory);
        testProduct.setActive(true);
        testProduct.setDeleted(false);
        testProduct.setCreatedAt(LocalDateTime.now());
        testProduct.setUpdatedAt(LocalDateTime.now());
        testProduct.setVariants(new ArrayList<>(List.of(testVariant)));
    }

    @Nested
    @DisplayName("createProduct Tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully with valid request")
        void shouldCreateProductSuccessfully() throws Exception {
            // Given
            ProductRequest request = createValidProductRequest();
            when(categoryRepo.findById(1L)).thenReturn(Optional.of(testCategory));
            when(productRepo.save(any(Product.class))).thenReturn(testProduct);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            ProductResponse response = productService.createProduct(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Test Product");
            verify(productRepo).save(any(Product.class));
            verify(outboxRepo).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void shouldThrowExceptionWhenCategoryNotFound() {
            // Given
            ProductRequest request = createValidProductRequest();
            when(categoryRepo.findById(1L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found");
        }

        @Test
        @DisplayName("Should throw exception when variant has no primary image")
        void shouldThrowExceptionWhenNoPrimaryImage() {
            // Given
            ProductRequest request = createProductRequestWithoutPrimaryImage();
            when(categoryRepo.findById(1L)).thenReturn(Optional.of(testCategory));

            // When/Then
            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly one primary image");
        }

        @Test
        @DisplayName("Should throw exception when variant has multiple primary images")
        void shouldThrowExceptionWhenMultiplePrimaryImages() {
            // Given
            ProductRequest request = createProductRequestWithMultiplePrimaryImages();
            when(categoryRepo.findById(1L)).thenReturn(Optional.of(testCategory));

            // When/Then
            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly one primary image");
        }
    }

    @Nested
    @DisplayName("getProductById Tests")
    class GetProductByIdTests {

        @Test
        @DisplayName("Should return product when found")
        void shouldReturnProductWhenFound() {
            // Given
            when(productRepo.findById(1L)).thenReturn(Optional.of(testProduct));

            // When
            ProductResponse response = productService.getProductById(1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Test Product");
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            // Given
            when(productRepo.findById(99L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> productService.getProductById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product not found");
        }
    }

    @Nested
    @DisplayName("getProductBySku Tests")
    class GetProductBySkuTests {

        @Test
        @DisplayName("Should return product by SKU")
        void shouldReturnProductBySku() {
            // Given
            when(productRepo.findByVariantSku("SKU-001")).thenReturn(Optional.of(testProduct));

            // When
            ProductSkuResponse response = productService.getProductBySku("SKU-001");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProductId()).isEqualTo(1L);
            assertThat(response.getSelectedVariant().getSku()).isEqualTo("SKU-001");
        }

        @Test
        @DisplayName("Should throw exception when SKU not found")
        void shouldThrowExceptionWhenSkuNotFound() {
            // Given
            when(productRepo.findByVariantSku("INVALID")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> productService.getProductBySku("INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product with SKU INVALID not found");
        }
    }

    // Helper methods
    private ProductRequest createValidProductRequest() {
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

    private ProductRequest createProductRequestWithoutPrimaryImage() {
        ProductRequest.ImageDto imageDto = new ProductRequest.ImageDto();
        imageDto.setUrl("https://example.com/image.jpg");
        imageDto.setPublicId("img-001");
        imageDto.setPrimary(false);

        ProductRequest.VariantDto variantDto = new ProductRequest.VariantDto();
        variantDto.setSku("SKU-001");
        variantDto.setPrice(new BigDecimal("999.99"));
        variantDto.setImages(List.of(imageDto));

        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setCategoryId(1L);
        request.setVariants(List.of(variantDto));

        return request;
    }

    private ProductRequest createProductRequestWithMultiplePrimaryImages() {
        ProductRequest.ImageDto imageDto1 = new ProductRequest.ImageDto();
        imageDto1.setUrl("https://example.com/image1.jpg");
        imageDto1.setPrimary(true);

        ProductRequest.ImageDto imageDto2 = new ProductRequest.ImageDto();
        imageDto2.setUrl("https://example.com/image2.jpg");
        imageDto2.setPrimary(true);

        ProductRequest.VariantDto variantDto = new ProductRequest.VariantDto();
        variantDto.setSku("SKU-001");
        variantDto.setPrice(new BigDecimal("999.99"));
        variantDto.setImages(List.of(imageDto1, imageDto2));

        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setCategoryId(1L);
        request.setVariants(List.of(variantDto));

        return request;
    }
}
