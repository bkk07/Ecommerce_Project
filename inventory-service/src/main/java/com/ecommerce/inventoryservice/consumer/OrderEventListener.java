package com.ecommerce.inventoryservice.consumer;

import com.ecommerce.inventory.InventoryLockEvent;
import com.ecommerce.inventory.InventoryLockFailedEvent;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.ecommerce.order.OrderCancelEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.ecommerce.common.KafkaProperties.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = ORDER_CANCEL_EVENTS_TOPIC, groupId = "inventory-saga-group")
    public void handleOrderCancelled(String eventString) {
        try {
            OrderCancelEvent event = objectMapper.readValue(eventString, OrderCancelEvent.class);
            log.info("Received OrderCancelEvent for Order: {}", event.getOrderId());
            // Idempotency is handled inside service or by checking processed events if needed
            // Here we iterate over items and release stock
            event.getItems().forEach(item -> {
                try {
                    inventoryService.releaseStock(item.getSkuCode(), item.getQuantity(), event.getOrderId());
                } catch (Exception e) {
                    log.error("Failed to release stock for SKU {} in Order {}", item.getSkuCode(), event.getOrderId(), e);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize OrderCancelEvent", e);
            throw new RuntimeException("Deserialization failed", e);
        }
    }

    @KafkaListener(topics = INVENTORY_LOCK_TOPIC, groupId = "inventory-group")
    public void handleInventoryLock(String eventString) {
        InventoryLockEvent event = null;
        try {
            event = objectMapper.readValue(eventString, InventoryLockEvent.class);
            log.info("Received InventoryLockEvent for Order: {}", event.getOrderId());
            inventoryService.lockStockForOrder(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize InventoryLockEvent", e);
        } catch (Exception e) {
            log.error("Failed to lock stock for order: {}", event != null ? event.getOrderId() : "unknown", e);
            if (event != null) {
                InventoryLockFailedEvent failedEvent = new InventoryLockFailedEvent(event.getOrderId(), e.getMessage());
                kafkaTemplate.send(INVENTORY_LOCK_FAILED_TOPIC, event.getOrderId(), failedEvent);
            }
        }
    }

}
