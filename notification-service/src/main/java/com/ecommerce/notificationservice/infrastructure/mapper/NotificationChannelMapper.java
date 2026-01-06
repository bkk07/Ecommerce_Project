package com.ecommerce.notificationservice.infrastructure.mapper;

import com.ecommerce.notification.ChannelType;

/**
 * Maps shared notification channel enum (from common-dtos) to
 * notification-service's internal domain enum.
 */
public final class NotificationChannelMapper {

    private NotificationChannelMapper() {
    }

    public static com.ecommerce.notificationservice.domain.enumtype.ChannelType toDomain(ChannelType channel) {
        if (channel == null) return null;
        return switch (channel) {
            case EMAIL -> com.ecommerce.notificationservice.domain.enumtype.ChannelType.EMAIL;
            case PHONE -> com.ecommerce.notificationservice.domain.enumtype.ChannelType.PHONE;
            case SMS -> com.ecommerce.notificationservice.domain.enumtype.ChannelType.SMS;
        };
    }
}

