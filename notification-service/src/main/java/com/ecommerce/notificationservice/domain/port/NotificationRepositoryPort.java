package com.ecommerce.notificationservice.domain.port;

import com.ecommerce.notificationservice.domain.model.NotificationLog;

public interface NotificationRepositoryPort {
    NotificationLog save(NotificationLog log);
}