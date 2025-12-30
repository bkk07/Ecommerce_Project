package com.ecommerce.searchservice.consumer;

import com.ecommerce.inventory.InventoryUpdatedEvent;
import com.ecommerce.product.ProductCreatedEvent;
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
    @KafkaListener(topics = PRODUCT_EVENTS_TOPIC, groupId = PRODUCT_EVENTS_GROUP)
    public void handleProductCreated(ProductCreatedEvent event) {
        log.info("Indexing new product: {}", event.getName());
        ProductDocument product = ProductDocument.builder()
                .id(String.valueOf(event.getProductId()))
                .skuCode(event.getSku())
                .name(event.getName())
                .description(event.getDescription())
                .price(event.getPrice())
                .isInStock(false) // Default to false
                .build();

        productRepository.save(product);
        log.info("Product indexed successfully.");
    }

    // 2. INVENTORY UPDATED -> Update 'isInStock' in Elasticsearch
    @KafkaListener(topics = INVENTORY_EVENTS_TOPIC, groupId = INVENTORY_EVENTS_GROUP)
    public void handleInventoryUpdate(InventoryUpdatedEvent event) {
        log.info("Received Inventory Update for SKU: {}", event.getSkuCode());
        productRepository.findBySkuCode(event.getSkuCode()).ifPresentOrElse(
                product -> {
                    product.setInStock(event.isAvailable());
                    productRepository.save(product);
                    log.info("Updated stock status for SKU: {}", event.getSkuCode());
                },
                () -> log.error("Product not found in Elasticsearch for SKU: {}", event.getSkuCode())
        );
    }
}