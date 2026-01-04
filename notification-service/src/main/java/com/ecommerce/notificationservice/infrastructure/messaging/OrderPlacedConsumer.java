package com.ecommerce.notificationservice.infrastructure.messaging;
import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import com.ecommerce.notificationservice.infrastructure.entity.UserProfileEntity;
import com.ecommerce.notificationservice.infrastructure.events.NotificationEvent;
import com.ecommerce.notificationservice.infrastructure.repository.JpaUserProfileRepository;
import com.ecommerce.notificationservice.service.NotificationService;
import com.ecommerce.order.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPlacedConsumer {

    private final JpaUserProfileRepository userProfileRepository;
    private final NotificationService notificationService;

    @KafkaListener(topics = "order-placed-event", groupId = "notification-order-group", properties = "spring.json.value.default.type=com.ecommerce.order.OrderPlacedEvent")
    public void consumeOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent: {}", event.getOrderId());

        // 1. Fetch User Details from Local DB
        UserProfileEntity user = userProfileRepository.findById(Long.valueOf(event.getUserId()))
                .orElse(null);

        if (user == null) {
            log.warn("User profile not found for userId: {}. Cannot send notification.", event.getUserId());
            return;
        }

        // 2. Prepare Notification Payload
        Map<String, String> payload = new HashMap<>();
        payload.put("name", user.getName());
        payload.put("orderId", event.getOrderId());

        // 3. Send Notification
        NotificationEvent notificationEvent = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_PLACED")
                .recipient(user.getEmail())
                .channel(ChannelType.EMAIL)
                .payload(payload)
                .occurredAt(LocalDateTime.now())
                .build();
        notificationService.processNotification(notificationEvent);
        log.info("Processed Order Placed Notification for Order ID: {}", event.getOrderId());
    }
}
