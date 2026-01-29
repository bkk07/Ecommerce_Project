package com.ecommerce.notificationservice.infrastructure.adpater;

import com.ecommerce.notificationservice.domain.model.NotificationLog;
import com.ecommerce.notificationservice.domain.port.NotificationRepositoryPort;
import com.ecommerce.notificationservice.infrastructure.entity.NotificationLogEntity;
import com.ecommerce.notificationservice.infrastructure.repository.JpaNotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final JpaNotificationLogRepository jpaRepository;

    @Override
    public NotificationLog save(NotificationLog log) {
        // Domain -> Entity
        NotificationLogEntity entity = NotificationLogEntity.builder()
                .id(log.getId())
                .eventId(log.getEventId())
                .recipient(log.getRecipient())
                .content(log.getContent())
                .channelType(log.getChannelType())
                .status(log.getStatus())
                .retryCount(log.getRetryCount())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();

        // Save
        NotificationLogEntity saved = jpaRepository.save(entity);

        // Entity -> Domain (Return with ID)
        log.setId(saved.getId());
        return log;
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return jpaRepository.existsByEventId(eventId);
    }
}