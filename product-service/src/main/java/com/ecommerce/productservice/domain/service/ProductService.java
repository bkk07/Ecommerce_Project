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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final OutboxRepository outboxRepo;
    private final ObjectMapper objectMapper; // For JSON serialization

    @Transactional
    @CacheEvict(value = "products", key = "#result.id") // Clear cache on create/update
    public ProductResponse createProduct(ProductRequest request) {

        // 1. Validate Category
        Category category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // 2. Map Entity
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setCategory(category);

        // 3. Map Variants
        request.getVariants().forEach(v -> {
            ProductVariant variant = new ProductVariant();
            variant.setSku(v.getSku());
            variant.setPrice(v.getPrice());
            variant.setSpecs(v.getSpecs());
            product.addVariant(variant);
        });

        // 4. Map Images (URLs from Cloudinary)
        if (request.getImages() != null) {
            request.getImages().forEach(imgDto -> {
                ProductImage img = new ProductImage();
                img.setUrl(imgDto.getUrl());
                img.setPublicId(imgDto.getPublicId());
                img.setPrimary(imgDto.isPrimary());
                product.addImage(img);
            });
        }

        // 5. Save to DB (ACID)
        Product savedProduct = productRepo.save(product);

        // 6. 🚀 Transactional Outbox Pattern
        // Save the event to the DB in the same transaction. Guaranteed delivery.
        saveOutboxEvent(savedProduct);

        return mapToResponse(savedProduct);
    }

    private void saveOutboxEvent(Product product) {
        List<OutboxEvent> outboxEvents = new java.util.ArrayList<>();
        try {
            for (ProductVariant variant : product.getVariants()) {
                ProductCreatedEvent event = new ProductCreatedEvent(
                        product.getId(),
                        //here name and the Description are not required in the future i can remove them first in the common dtos and i  need to delete this also
                        product.getName(),
                        variant.getSku(),
                        product.getDescription(),
                        variant.getPrice()
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
            throw new RuntimeException("Error serializing event", e);
        }
    }

    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapToResponse(product);
    }

    // (Helper methods for mapping would go here or in a Mapper class)
    // Inside ProductService.java

    private ProductResponse mapToResponse(Product p) {
        // 1. Map Variants
        List<ProductResponse.VariantResponse> variantDtos = p.getVariants().stream()
                .map(v -> ProductResponse.VariantResponse.builder()
                        .id(v.getId())
                        .sku(v.getSku())
                        .price(v.getPrice())
                        .specs(v.getSpecs())
                        .build())
                .toList();

        // 2. Map Images
        List<ProductResponse.ImageResponse> imageDtos = p.getImages().stream()
                .map(img -> ProductResponse.ImageResponse.builder()
                        .id(img.getId())
                        .url(img.getUrl())
                        .publicId(img.getPublicId())
                        .isPrimary(img.isPrimary())
                        .sortOrder(img.getSortOrder())
                        .build())
                .toList();

        // 3. Build Final Response
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .brand(p.getBrand())
                .categoryId(p.getCategory().getId())       // ID for routing
                .categoryName(p.getCategory().getName())   // Name for display
                .isActive(p.isActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .variants(variantDtos)
                .images(imageDtos)
                .build();
    }
}