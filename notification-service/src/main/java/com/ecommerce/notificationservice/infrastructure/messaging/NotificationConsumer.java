package com.ecommerce.notificationservice.infrastructure.messaging;

import com.ecommerce.notificationservice.domain.port.NotificationRepositoryPort;
import com.ecommerce.notificationservice.infrastructure.events.NotificationEvent; // Ensure this import is correct
import com.ecommerce.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final NotificationRepositoryPort notificationRepository;

    // NOTE: ObjectMapper is removed. Spring deserializes JSON -> NotificationEvent automatically now.

    // 1. High Priority (Urgent)
    @KafkaListener(topics = "notifications.urgent", groupId = "notification-group")
    public void consumeUrgent(NotificationEvent event, Acknowledgment ack) { // <--- CHANGED FROM String TO NotificationEvent
        processMessage(event, "URGENT", ack);
    }

    // 2. Medium Priority (Transactional)
    @KafkaListener(topics = "notifications.transactional", groupId = "notification-group")
    public void consumeTransactional(NotificationEvent event, Acknowledgment ack) { // <--- CHANGED FROM String TO NotificationEvent
        processMessage(event, "TRANSACTIONAL", ack);
    }

    // 3. Low Priority (Marketing)
    @KafkaListener(topics = "notifications.marketing", groupId = "notification-group")
    public void consumeMarketing(NotificationEvent event, Acknowledgment ack) { // <--- CHANGED FROM String TO NotificationEvent
        processMessage(event, "MARKETING", ack);
    }

    // Helper Method
    private void processMessage(NotificationEvent event, String priority, Acknowledgment ack) {
        try {
            log.info("[{}] Received EventID: {}", priority, event.getEventId());

            // Idempotency Check
            if (notificationRepository.existsByEventId(event.getEventId())) {
                log.warn("Duplicate Event Detected (EventID: {}). Skipping.", event.getEventId());
                ack.acknowledge();
                return;
            }

            // Process
            notificationService.processNotification(event);

            // Acknowledge
            ack.acknowledge();
            log.info("[{}] Successfully processed EventID: {}", priority, event.getEventId());

        } catch (Exception e) {
            log.error("[{}] Error processing message. Sending to Retry/DLQ...", priority, e);
            throw new RuntimeException("Consumer processing failed", e);
        }
    }
}