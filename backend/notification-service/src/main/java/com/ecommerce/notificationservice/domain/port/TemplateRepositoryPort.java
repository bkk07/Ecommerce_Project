package com.ecommerce.notificationservice.domain.port;

import com.ecommerce.notificationservice.domain.model.NotificationTemplate;
import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import java.util.Optional;

public interface TemplateRepositoryPort {
    Optional<NotificationTemplate> findByEventTypeAndChannel(String eventType, ChannelType channelType);
}