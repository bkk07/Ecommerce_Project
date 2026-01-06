package com.ecommerce.userservice.infrastructure.messaging;

import com.ecommerce.event.UserEvent;
import com.ecommerce.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    // Define your topics
    private static final String TOPIC_URGENT = "notifications.urgent";
    private static final String TOPIC_TRANSACTIONAL = "notifications.transactional";
    private static final String TOPIC_USER_EVENTS = "user-events";

    public void sendNotification(NotificationEvent request) {
        String topic = determineTopic(request.getEventType());
        log.info("Sending Kafka Event: {} to Topic: {}", request.getEventType(), topic);
        kafkaTemplate.send(topic, request);
    }

    public void sendUserEvent(UserEvent event) {
        log.info("Sending User Event: {} to Topic: {}", event.getEventType(), TOPIC_USER_EVENTS);
        kafkaTemplate.send(TOPIC_USER_EVENTS, event);
    }

    private String determineTopic(String eventType) {
        // High priority events go to 'urgent', others to 'transactional'
        if ("VERIFY_EMAIL_OTP".equals(eventType) ||
                "VERIFY_PHONE_OTP".equals(eventType) ||
                "FORGOT_PASSWORD_OTP".equals(eventType)) {
            return TOPIC_URGENT;
        }
        return TOPIC_TRANSACTIONAL;
    }
}
