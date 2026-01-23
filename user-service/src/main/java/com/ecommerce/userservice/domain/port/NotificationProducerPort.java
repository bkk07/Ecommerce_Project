package com.ecommerce.userservice.domain.port;

import com.ecommerce.event.UserEvent;
import com.ecommerce.notification.NotificationEvent;

public interface NotificationProducerPort {
    void sendNotification(NotificationEvent request);
    void sendUserEvent(UserEvent event);
}
