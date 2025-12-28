package com.ecommerce.notificationservice.infrastructure.messaging;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    // This is for the important notifications like email verification and mobile verification
    // High Priority
    @KafkaListener(topics = "notifications.urgent", groupId = "notif-group", containerFactory = "urgentFactory")
    public void consumeUrgent(String message) {
        processMessage(message, "URGENT");
    }

    // This is for the notifications like Order created
    // Medium Priority
    @KafkaListener(topics = "notifications.transactional", groupId = "notif-group", containerFactory = "transactionalFactory")
    public void consumeTransactional(String message) {
        processMessage(message, "TRANSACTIONAL");
    }

    // For Promotions
    // Low Priority
    @KafkaListener(topics = "notifications.marketing", groupId = "notif-group", containerFactory = "marketingFactory")
    public void consumeMarketing(String message) {
        processMessage(message, "MARKETING");
    }

    private void processMessage(String message, String priority) {
        try {
            log.info("[{}] Received: {}", priority, message);
            NotificationRequest request = objectMapper.readValue(message, NotificationRequest.class);
            notificationService.processNotification(request);
        } catch (Exception e) {
            log.error("Error processing Kafka message", e);
            // Ideally, send to Dead Letter Queue (DLQ) here
        }
    }
}