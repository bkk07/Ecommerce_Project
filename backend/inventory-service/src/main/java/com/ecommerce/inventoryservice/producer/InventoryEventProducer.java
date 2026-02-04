package com.ecommerce.inventoryservice.producer;

import com.ecommerce.inventory.InventoryLockFailedEvent;
import com.ecommerce.inventory.InventoryReleasedEvent;
import com.ecommerce.inventory.InventoryUpdatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.kafka.support.KafkaHeaders;

import static com.ecommerce.common.KafkaProperties.*;

/**
 * Direct Kafka producer for inventory events.
 * 
 * <p><b>Note:</b> This service primarily uses the Transactional Outbox Pattern 
 * for reliable event publishing. This producer is available for scenarios 
 * where immediate, best-effort delivery is acceptable (e.g., real-time notifications).
 * 
 * <p>For critical business events, prefer using the OutboxPublisherJob pattern
 * via InventoryService.saveToOutbox() method.
 * 
 * @see com.ecommerce.inventoryservice.job.OutboxPublisherJob
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Sends an inventory update event directly to Kafka.
     * Uses circuit breaker and retry for resilience.
     * 
     * @param event The inventory updated event
     * @throws RuntimeException if serialization or sending fails
     */
    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "sendInventoryUpdateFallback")
    @Retry(name = "kafkaProducer")
    public void sendInventoryUpdate(InventoryUpdatedEvent event) {
        log.info("Sending InventoryUpdatedEvent for SKU: {}", event.getSkuCode());
        try {
            String eventString = objectMapper.writeValueAsString(event);
            Message<String> message = MessageBuilder
                    .withPayload(eventString)
                    .setHeader(KafkaHeaders.TOPIC, INVENTORY_EVENTS_TOPIC)
                    .setHeader(KafkaHeaders.KEY, event.getSkuCode())
                    .build();

            kafkaTemplate.send(message).join(); // Sync send for reliability
        } catch (JsonProcessingException e) {
            log.error("Error serializing InventoryUpdatedEvent: {}", e.getMessage());
            throw new RuntimeException("Error serializing InventoryUpdatedEvent", e);
        }
    }

    /**
     * Fallback when circuit breaker is open for inventory updates.
     */
    public void sendInventoryUpdateFallback(InventoryUpdatedEvent event, Exception e) {
        log.warn("Failed to send InventoryUpdatedEvent for SKU: {}. Circuit breaker open. Error: {}", 
                event.getSkuCode(), e.getMessage());
        // Event will be handled via outbox pattern as backup
    }

    /**
     * Sends an inventory released event directly to Kafka.
     * Uses circuit breaker and retry for resilience.
     * 
     * @param event The inventory released event
     * @throws RuntimeException if serialization or sending fails
     */
    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "sendInventoryReleasedFallback")
    @Retry(name = "kafkaProducer")
    public void sendInventoryReleased(InventoryReleasedEvent event) {
        log.info("Sending InventoryReleasedEvent for Order: {}", event.getOrderId());
        try {
            String eventString = objectMapper.writeValueAsString(event);
            Message<String> message = MessageBuilder
                    .withPayload(eventString)
                    .setHeader(KafkaHeaders.TOPIC, INVENTORY_RELEASED_EVENTS_TOPIC)
                    .setHeader(KafkaHeaders.KEY, event.getOrderId())
                    .build();
            
            kafkaTemplate.send(message).join(); // Sync send for reliability
        } catch (JsonProcessingException e) {
            log.error("Error serializing InventoryReleasedEvent: {}", e.getMessage());
            throw new RuntimeException("Error serializing InventoryReleasedEvent", e);
        }
    }

    /**
     * Fallback when circuit breaker is open for inventory released events.
     */
    public void sendInventoryReleasedFallback(InventoryReleasedEvent event, Exception e) {
        log.warn("Failed to send InventoryReleasedEvent for Order: {}. Circuit breaker open. Error: {}", 
                event.getOrderId(), e.getMessage());
    }

    /**
     * Sends an inventory lock failed event directly to Kafka.
     * Uses circuit breaker and retry for resilience.
     * 
     * @param event The inventory lock failed event
     * @throws RuntimeException if serialization or sending fails
     */
    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "sendInventoryLockFailedFallback")
    @Retry(name = "kafkaProducer")
    public void sendInventoryLockFailed(InventoryLockFailedEvent event) {
        log.info("Sending InventoryLockFailedEvent for Order: {}", event.getOrderId());
        try {
            String eventString = objectMapper.writeValueAsString(event);
            Message<String> message = MessageBuilder
                    .withPayload(eventString)
                    .setHeader(KafkaHeaders.TOPIC, INVENTORY_LOCK_FAILED_TOPIC)
                    .setHeader(KafkaHeaders.KEY, event.getOrderId())
                    .build();
            
            kafkaTemplate.send(message).join(); // Sync send for reliability
        } catch (JsonProcessingException e) {
            log.error("Error serializing InventoryLockFailedEvent: {}", e.getMessage());
            throw new RuntimeException("Error serializing InventoryLockFailedEvent", e);
        }
    }

    /**
     * Fallback when circuit breaker is open for lock failed events.
     */
    public void sendInventoryLockFailedFallback(InventoryLockFailedEvent event, Exception e) {
        log.warn("Failed to send InventoryLockFailedEvent for Order: {}. Circuit breaker open. Error: {}", 
                event.getOrderId(), e.getMessage());
    }
}
