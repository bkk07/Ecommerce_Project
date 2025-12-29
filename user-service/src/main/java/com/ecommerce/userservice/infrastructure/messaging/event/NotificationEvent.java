package com.ecommerce.userservice.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

// In a shared library or copied to both services
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventId;
    private String eventType;
    private String recipient;
    private ChannelType channel;
    private Map<String, String> payload;
    private LocalDateTime occurredAt;
}