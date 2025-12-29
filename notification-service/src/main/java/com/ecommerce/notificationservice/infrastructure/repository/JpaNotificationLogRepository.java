package com.ecommerce.notificationservice.infrastructure.repository;

import com.ecommerce.notificationservice.infrastructure.entity.NotificationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaNotificationLogRepository extends JpaRepository<NotificationLogEntity, Long> {
    boolean existsByEventId(String eventId);
    // You can add custom finders here if needed, e.g.:
    // List<NotificationLogEntity> findByStatus(NotificationStatus status);
}