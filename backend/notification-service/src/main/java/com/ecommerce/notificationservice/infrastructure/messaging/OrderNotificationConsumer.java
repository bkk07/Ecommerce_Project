package com.ecommerce.notificationservice.infrastructure.messaging;

import com.ecommerce.notification.ChannelType;
import com.ecommerce.notification.NotificationEvent;
import com.ecommerce.notificationservice.infrastructure.entity.ProcessedEventEntity;
import com.ecommerce.notificationservice.infrastructure.entity.UserProfileEntity;
import com.ecommerce.notificationservice.infrastructure.repository.JpaUserProfileRepository;
import com.ecommerce.notificationservice.infrastructure.repository.ProcessedEventRepository;
import com.ecommerce.notificationservice.service.NotificationService;
import com.ecommerce.order.OrderNotificationEvent;
import com.ecommerce.order.OrderNotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.ecommerce.common.KafkaProperties.ORDER_NOTIFICATIONS_TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationConsumer {

    private final JpaUserProfileRepository userProfileRepository;
    private final NotificationService notificationService;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = ORDER_NOTIFICATIONS_TOPIC, groupId = "notification-order-group", properties = "spring.json.value.default.type=com.ecommerce.order.OrderNotificationEvent")
    @Transactional
    public void consumeOrderNotification(OrderNotificationEvent event, Acknowledgment acknowledgment) {
        log.info("Received OrderNotificationEvent: {} Type: {}", event.getEventId(), event.getType());

        try {
            // 1. Idempotency Check
            if (processedEventRepository.existsById(event.getEventId())) {
                log.info("Event {} already processed. Skipping.", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            // 2. Fetch User Details
            UserProfileEntity user = userProfileRepository.findById(Long.valueOf(event.getUserId()))
                    .orElse(null);

            if (user == null) {
                log.warn("User profile not found for userId: {}. Cannot send notification.", event.getUserId());
                acknowledgment.acknowledge(); // Acknowledge to avoid infinite loop if user is missing
                return;
            }

            // 3. Prepare Notification Payload based on Type
            Map<String, String> payload = new HashMap<>();
            payload.put("name", user.getName());
            payload.put("orderId", event.getPayload().getOrderId());
            payload.put("totalAmount", String.valueOf(event.getPayload().getTotalAmount()));
            
            // Add reason for cancelled and refunded notifications
            if (event.getType() == OrderNotificationType.ORDER_CANCELLED ||
                event.getType() == OrderNotificationType.ORDER_REFUNDED) {
                String reason = event.getPayload().getCancellationReason();
                payload.put("reason", reason != null ? reason : "N/A");
            }

            // 4. Send Notification
                NotificationEvent notificationEvent = new NotificationEvent();
            notificationEvent.setEventId(UUID.randomUUID().toString());
            notificationEvent.setEventType(event.getType().name());
            notificationEvent.setRecipient(user.getEmail());
            notificationEvent.setChannel(ChannelType.EMAIL);
            notificationEvent.setPayload(payload);
            notificationEvent.setOccurredAt(LocalDateTime.now());

            notificationService.processNotification(notificationEvent);

            // 5. Mark Event as Processed
            processedEventRepository.save(new ProcessedEventEntity(event.getEventId(), LocalDateTime.now()));
            
            log.info("Processed Order Notification for Order ID: {}", event.getPayload().getOrderId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing notification event: {}", event.getEventId(), e);
            // Do NOT acknowledge if you want to retry. 
            // Or acknowledge if you want to skip poison pills (depending on policy).
            // For now, we let it fail so it can be retried or go to DLQ.
            throw e;
        }
    }
}
