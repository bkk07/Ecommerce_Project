package com.ecommerce.notificationservice.infrastructure.messaging;

import com.ecommerce.notification.NotificationEvent;
import com.ecommerce.notificationservice.domain.port.NotificationRepositoryPort;
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

    // 1. High Priority (Urgent)
    @KafkaListener(topics = "notifications.urgent", groupId = "notification-group", properties = "spring.json.value.default.type=com.ecommerce.notification.NotificationEvent")
    public void consumeUrgent(NotificationEvent event, Acknowledgment ack) {
        processMessage(event, "URGENT", ack);
    }

    // 2. Medium Priority (Transactional)
    @KafkaListener(topics = "notifications.transactional", groupId = "notification-group", properties = "spring.json.value.default.type=com.ecommerce.notification.NotificationEvent")
    public void consumeTransactional(NotificationEvent event, Acknowledgment ack) {
        processMessage(event, "TRANSACTIONAL", ack);
    }

    // 3. Low Priority (Marketing)
    @KafkaListener(topics = "notifications.marketing", groupId = "notification-group", properties = "spring.json.value.default.type=com.ecommerce.notification.NotificationEvent")
    public void consumeMarketing(NotificationEvent event, Acknowledgment ack) {
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
