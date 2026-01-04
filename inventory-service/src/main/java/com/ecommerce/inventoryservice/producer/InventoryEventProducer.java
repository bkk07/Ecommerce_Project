package com.ecommerce.inventoryservice.producer;
import com.ecommerce.inventory.InventoryLockFailedEvent;
import com.ecommerce.inventory.InventoryReleasedEvent;
import com.ecommerce.inventory.InventoryUpdatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.kafka.support.KafkaHeaders;

import static com.ecommerce.common.KafkaProperties.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendInventoryUpdate(InventoryUpdatedEvent event) {
        log.info("Sending InventoryUpdatedEvent for SKU: {}", event.getSkuCode());
        try {
            String eventString = objectMapper.writeValueAsString(event);
            Message<String> message = MessageBuilder
                    .withPayload(eventString)
                    .setHeader(KafkaHeaders.TOPIC, INVENTORY_EVENTS_TOPIC)
                    .build();

            kafkaTemplate.send(message);
        }catch (JsonProcessingException e) {
            log.error("Error serializing InventoryUpdatedEvent: {}", e.getMessage());
            throw new RuntimeException("Error serializing InventoryUpdatedEvent", e);
        }
    }

    public void sendInventoryReleased(InventoryReleasedEvent event) {
        log.info("Sending InventoryReleasedEvent for Order: {}", event.getOrderId());
        try {
            String eventString = objectMapper.writeValueAsString(event);
            Message<String> message = MessageBuilder
                    .withPayload(eventString)
                    .setHeader(KafkaHeaders.TOPIC, INVENTORY_RELEASED_EVENTS_TOPIC)
                    .build();
            kafkaTemplate.send(message);
        } catch (JsonProcessingException e) {
            log.error("Error serializing InventoryReleasedEvent: {}", e.getMessage());
        }
    }

    public void sendInventoryLockFailed(InventoryLockFailedEvent event) {
        log.info("Sending InventoryLockFailedEvent for Order: {}", event.getOrderId());
        try {
            String eventString = objectMapper.writeValueAsString(event);
            Message<String> message = MessageBuilder
                    .withPayload(eventString)
                    .setHeader(KafkaHeaders.TOPIC, INVENTORY_LOCK_FAILED_TOPIC)
                    .build();
            kafkaTemplate.send(message);
        } catch (JsonProcessingException e) {
            log.error("Error serializing InventoryLockFailedEvent: {}", e.getMessage());
        }
    }
}
