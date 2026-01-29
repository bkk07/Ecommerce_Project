package com.ecommerce.notificationservice.infrastructure.repository;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import com.ecommerce.notificationservice.infrastructure.entity.NotificationTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface JpaTemplateRepository extends JpaRepository<NotificationTemplateEntity, Long> {
    // This is the method used by TemplateRepositoryAdapter
    Optional<NotificationTemplateEntity> findByEventTypeAndChannelType(String eventType, ChannelType channelType);
}