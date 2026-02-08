package com.ecommerce.searchservice.consumer;

import com.ecommerce.inventory.InventoryUpdatedEvent;
import com.ecommerce.product.ProductCreatedEvent;
import com.ecommerce.rating.RatingUpdatedEvent;
import com.ecommerce.searchservice.model.ProductDocument;
import com.ecommerce.searchservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static com.ecommerce.common.KafkaProperties.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchConsumer {

    private final ProductRepository productRepository;

    // 1. PRODUCT CREATED -> Insert into Elasticsearch
    @KafkaListener(topics = PRODUCT_EVENTS_TOPIC, groupId ="search-event-group")
    public void handleProductCreated(ProductCreatedEvent event) {
        log.info("Indexing new product: {} with SKU: {} and ID: {}", event.getName(), event.getSku(), event.getProductId());
        ProductDocument product = ProductDocument.builder()
                .id(event.getSku()) // Use SKU as the document ID to prevent overwriting variants
                .productId(event.getProductId())
                .skuCode(event.getSku())
                .name(event.getName())
            .nameKeyword(event.getName())
                .description(event.getDescription())
                .price(event.getPrice())
                .isInStock(false)
                .categories(event.getCategories())
                .brand(event.getBrand())
                .imageUrl(event.getImageUrl())
                .averageRating(0.0)
                .totalRatings(0L)
                .build();

        productRepository.save(product);
        log.info("Product indexed successfully for SKU: {}", event.getSku());
    }

    // 2. INVENTORY UPDATED -> Update 'isInStock' in Elasticsearch
    @KafkaListener(topics = INVENTORY_EVENTS_TOPIC, groupId = "search-event-group")
    public void handleInventoryUpdate(InventoryUpdatedEvent event) {
        log.info("Received Inventory Update for SKU: {}", event.getSkuCode());
        
        // Use findById since we are now using SKU as the document ID
        productRepository.findById(event.getSkuCode()).ifPresentOrElse(
                product -> {
                    product.setInStock(event.isAvailable());
                    productRepository.save(product);
                    log.info("Updated stock status for SKU: {} (Doc ID: {})", event.getSkuCode(), product.getId());
                },
                () -> log.error("Product not found in Elasticsearch for SKU: {}", event.getSkuCode())
        );
    }

    // 3. RATING UPDATED -> Update rating fields in Elasticsearch
    @KafkaListener(topics = RATING_EVENTS_TOPIC, groupId = "search-event-group")
    public void handleRatingUpdate(RatingUpdatedEvent event) {
        log.info("========================================");
        log.info("RECEIVED RATING UPDATE EVENT");
        log.info("SKU: {}", event.getSku());
        log.info("Average Rating: {}", event.getAverageRating());
        log.info("Total Ratings: {}", event.getTotalRatings());
        log.info("========================================");
        
        productRepository.findById(event.getSku()).ifPresentOrElse(
                product -> {
                    product.setAverageRating(event.getAverageRating());
                    product.setTotalRatings(event.getTotalRatings());
                    productRepository.save(product);
                    log.info("Updated rating for SKU: {} - Avg: {}, Total: {}", 
                            event.getSku(), event.getAverageRating(), event.getTotalRatings());
                },
                () -> log.error("Product not found in Elasticsearch for SKU: {}", event.getSku())
        );
    }
}