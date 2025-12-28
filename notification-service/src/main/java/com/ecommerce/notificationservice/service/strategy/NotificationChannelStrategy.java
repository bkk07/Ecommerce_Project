package com.ecommerce.notificationservice.service.strategy;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;

public interface NotificationChannelStrategy {
    void send(String recipient, String message);
    ChannelType getSupportedType();
}