package com.ecommerce.productservice.domain.service;

import com.ecommerce.product.ProductCreatedEvent;
import com.ecommerce.productservice.api.dto.ProductRequest;
import com.ecommerce.productservice.api.dto.ProductResponse;
import com.ecommerce.productservice.api.exception.ResourceNotFoundException;
import com.ecommerce.productservice.domain.entity.*;
import com.ecommerce.productservice.domain.repository.CategoryRepository;
import com.ecommerce.productservice.domain.repository.OutboxRepository;
import com.ecommerce.productservice.domain.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
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
