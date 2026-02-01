package com.ecommerce.productservice.domain.service;

import com.ecommerce.product.*;
import com.ecommerce.productservice.api.dto.ProductRequest;
import com.ecommerce.productservice.api.dto.ProductResponse;
import com.ecommerce.productservice.api.dto.ProductSkuResponse;
import com.ecommerce.productservice.api.exception.ProductValidationException;
import com.ecommerce.productservice.api.exception.ResourceNotFoundException;
import com.ecommerce.productservice.domain.entity.*;
import com.ecommerce.productservice.domain.repository.CategoryRepository;
import com.ecommerce.productservice.domain.repository.OutboxRepository;
import com.ecommerce.productservice.domain.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final OutboxRepository outboxRepo;
    private final ObjectMapper objectMapper;

    // ===================== CREATE PRODUCT =====================
    @Transactional
    @CacheEvict(value = "products", key = "#result.id")
    public ProductResponse createProduct(ProductRequest request) {

        // 1️⃣ Validate Category
        Category category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // 2️⃣ Map Product
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setCategory(category);

        // 3️⃣ Map Variants + Variant Images
        request.getVariants().forEach(v -> {

            ProductVariant variant = new ProductVariant();
            variant.setSku(v.getSku());
            variant.setPrice(v.getPrice());
            variant.setSpecs(v.getSpecs());

            // 🔒 Enforce exactly ONE primary image per variant
            long primaryCount = v.getImages().stream()
                    .filter(ProductRequest.ImageDto::isPrimary)
                    .count();

            if (primaryCount != 1) {
                throw new IllegalArgumentException(
                        "Each variant must have exactly one primary image"
                );
            }

            // Map images to VARIANT
            v.getImages().forEach(imgDto -> {
                ProductImage img = new ProductImage();
                img.setUrl(imgDto.getUrl());
                img.setPublicId(imgDto.getPublicId());
                img.setPrimary(imgDto.isPrimary());
                variant.addImage(img);
            });

            product.addVariant(variant);
        });

        // 4️⃣ Save Product (ACID)
        Product savedProduct = productRepo.save(product);

        // 5️⃣ Transactional Outbox
        saveOutboxEvent(savedProduct);

        return mapToResponse(savedProduct);
    }

    // ===================== OUTBOX EVENT =====================
    private void saveOutboxEvent(Product product) {

        List<OutboxEvent> outboxEvents = new java.util.ArrayList<>();
        
        // Collect category hierarchy (Leaf -> Root)
        List<String> categoryHierarchy = new ArrayList<>();
        Category current = product.getCategory();
        while (current != null) {
            categoryHierarchy.add(current.getName());
            current = current.getParent();
        }
        // Reverse to get Root -> Leaf order (e.g., "Electronics", "Smartphones", "Apple")
        Collections.reverse(categoryHierarchy);

        try {
            for (ProductVariant variant : product.getVariants()) {

                String primaryImageUrl = variant.getImages().stream()
                        .filter(ProductImage::isPrimary)
                        .findFirst()
                        .map(ProductImage::getUrl)
                        .orElse(null);

                ProductCreatedEvent event = new ProductCreatedEvent(
                        product.getId(),
                        product.getName(),        // you can remove later as planned
                        variant.getSku(),
                        product.getDescription(),
                        variant.getPrice(),
                        primaryImageUrl,
                        categoryHierarchy
                );

                OutboxEvent outbox = OutboxEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .aggregateType("PRODUCT")
                        .aggregateId(product.getId().toString())
                        .type("PRODUCT_CREATED")
                        .payload(objectMapper.writeValueAsString(event))
                        .createdAt(java.time.LocalDateTime.now())
                        .processed(false)
                        .build();

                outboxEvents.add(outbox);
            }

            outboxRepo.saveAll(outboxEvents);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing product event", e);
        }
    }

    // ===================== GET PRODUCT =====================
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return mapToResponse(product);
    }

    // ===================== GET PRODUCT BY SKU =====================
    @Transactional(readOnly = true)
    public ProductSkuResponse getProductBySku(String sku) {
        Product product = productRepo.findByVariantSku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product with SKU " + sku + " not found"));

        ProductVariant selectedVariant = product.getVariants().stream()
                .filter(v -> v.getSku().equals(sku))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Variant with SKU " + sku + " not found"));

        List<ProductSkuResponse.ImageDto> images = selectedVariant.getImages().stream()
                .map(img -> ProductSkuResponse.ImageDto.builder()
                        .url(img.getUrl())
                        .isPrimary(img.isPrimary())
                        .build())
                .collect(Collectors.toList());

        ProductSkuResponse.SelectedVariant selectedVariantDto = ProductSkuResponse.SelectedVariant.builder()
                .sku(selectedVariant.getSku())
                .price(selectedVariant.getPrice())
                .specs(selectedVariant.getSpecs())
                .images(images)
                .averageRating(selectedVariant.getAverageRating())
                .ratingCount(selectedVariant.getRatingCount())
                .build();

        return ProductSkuResponse.builder()
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .categoryId(product.getCategory().getId())
                .selectedVariant(selectedVariantDto)
                .build();
    }

    // ===================== VALIDATE PRODUCTS =====================
    @Transactional(readOnly = true)
    public ProductValidationResponse validateProducts(
            List<ProductValidationItem> items) {

        List<ItemValidationResult> results = new ArrayList<>();
        log.info("Iam in validate products method in the prduct service ");
        boolean allValid = true;
        for (ProductValidationItem item : items) {

            ItemValidationResult result = new ItemValidationResult();
            result.setSkuCode(item.getSkuCode());

            Product product = productRepo.findByVariantSku(item.getSkuCode())
                    .orElse(null);

            if (product == null) {
                result.setValid(false);
                result.setReason(ProductValidationFailureReason.SKU_NOT_FOUND);
                allValid = false;
                results.add(result);
                continue;
            }

            if (!product.isActive()) {
                result.setValid(false);
                result.setReason(ProductValidationFailureReason.PRODUCT_DISABLED);
                allValid = false;
                results.add(result);
                continue;
            }

            if (product.isDeleted()) {
                result.setValid(false);
                result.setReason(ProductValidationFailureReason.PRODUCT_DELETED);
                allValid = false;
                results.add(result);
                continue;
            }

            ProductVariant variant = product.getVariants().stream()
                    .filter(v -> v.getSku().equals(item.getSkuCode()))
                    .findFirst()
                    .orElse(null);

            if (variant == null) {
                result.setValid(false);
                result.setReason(ProductValidationFailureReason.VARIANT_DELETED);
                allValid = false;
                results.add(result);
                continue;
            }

            if (variant.getPrice().compareTo(item.getPrice()) != 0) {
                result.setValid(false);
                result.setReason(ProductValidationFailureReason.PRICE_MISMATCH);
                result.setCurrentPrice(variant.getPrice());
                allValid = false;
                results.add(result);
                continue;
            }

            // ✅ Passed all checks
            result.setValid(true);
            results.add(result);
        }

        ProductValidationResponse response = new ProductValidationResponse();
        response.setValid(allValid);
        response.setResults(results);

        return response;
    }


    // ===================== RESPONSE MAPPER =====================
    private ProductResponse mapToResponse(Product p) {

        List<ProductResponse.VariantResponse> variantDtos =
                p.getVariants().stream()
                        .map(v -> ProductResponse.VariantResponse.builder()
                                .id(v.getId())
                                .sku(v.getSku())
                                .price(v.getPrice())
                                .specs(v.getSpecs())
                                .images(
                                        v.getImages().stream()
                                                .map(img -> ProductResponse.ImageResponse.builder()
                                                        .id(img.getId())
                                                        .url(img.getUrl())
                                                        .publicId(img.getPublicId())
                                                        .isPrimary(img.isPrimary())
                                                        .sortOrder(img.getSortOrder())
                                                        .build())
                                                .toList()
                                )
                                .build())
                        .toList();

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .brand(p.getBrand())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .isActive(p.isActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .variants(variantDtos)
                .build();
    }
}
