package com.ecommerce.notificationservice.infrastructure.adpater;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import com.ecommerce.notificationservice.domain.model.NotificationTemplate;
import com.ecommerce.notificationservice.domain.port.TemplateRepositoryPort;
import com.ecommerce.notificationservice.infrastructure.repository.JpaTemplateRepository; // You will need to create this Interface
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TemplateRepositoryAdapter implements TemplateRepositoryPort {

    private final JpaTemplateRepository jpaRepository;

    @Override
    public Optional<NotificationTemplate> findByEventTypeAndChannel(String eventType, ChannelType channelType) {
        // Map Entity -> Domain
        return jpaRepository.findByEventTypeAndChannelType(eventType, channelType)
                .map(entity -> NotificationTemplate.builder()
                        .eventType(entity.getEventType())
                        .channelType(entity.getChannelType())
                        .subject(entity.getSubject())
                        .bodyTemplate(entity.getBodyTemplate())
                        .build());
    }
}