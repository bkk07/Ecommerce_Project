package com.ecommerce.inventoryservice.consumer;

import com.ecommerce.inventoryservice.service.InventoryService;
import com.ecommerce.product.ProductCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.ecommerce.common.KafkaProperties.PRODUCT_EVENTS_TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventListener {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = PRODUCT_EVENTS_TOPIC, groupId ="inventory-event-group")
    public void handleProductCreated(String eventString) {
        try {
            ProductCreatedEvent event = objectMapper.readValue(eventString, ProductCreatedEvent.class);
            log.info("Received ProductCreatedEvent for SKU: {}", event.getSku());
            inventoryService.initStock(event.getSku());
        } catch (JsonProcessingException e) {
            log.error("Error deserializing ProductCreatedEvent: {}", e.getMessage());
            // Throwing exception to trigger DLQ if configured, or at least fail the offset commit
            throw new RuntimeException("Deserialization failed", e);
        }
    }
}
