package com.ecommerce.notificationservice.domain.model;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationTemplate {
    private String eventType; // e.g., ORDER_CREATED
    private ChannelType channelType;
    private String subject;
    private String bodyTemplate; // e.g., "Hi {name}, your OTP is {otp}"
}