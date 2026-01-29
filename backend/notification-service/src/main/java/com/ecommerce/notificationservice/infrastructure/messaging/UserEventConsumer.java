package com.ecommerce.notificationservice.infrastructure.messaging;

import com.ecommerce.event.UserEvent;
import com.ecommerce.notificationservice.infrastructure.entity.UserProfileEntity;
import com.ecommerce.notificationservice.infrastructure.repository.JpaUserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final JpaUserProfileRepository userProfileRepository;

    @KafkaListener(topics = "user-events", groupId = "notification-user-group", properties = "spring.json.value.default.type=com.ecommerce.event.UserEvent")
    public void consumeUserEvent(UserEvent event) {
        log.info("Received UserEvent: {}", event);
        UserProfileEntity entity = UserProfileEntity.builder()
                .userId(event.getUserId())
                .name(event.getName())
                .email(event.getEmail())
                .phone(event.getPhone())
                .build();
        userProfileRepository.save(entity);
        log.info("Updated User Profile for userId: {}", event.getUserId());
    }
}
