package com.ecommerce.notification;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Shared notification event DTO published over Kafka.
 *
 * Plain JavaBean (no Lombok) so it compiles in common-dtos without extra deps.
 */
public class NotificationEvent {
    private String eventId;
    private String eventType;
    private String recipient;
    private ChannelType channel;
    private Map<String, String> payload;
    private LocalDateTime occurredAt;

    public NotificationEvent() {
    }

    public NotificationEvent(String eventId,
                             String eventType,
                             String recipient,
                             ChannelType channel,
                             Map<String, String> payload,
                             LocalDateTime occurredAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.recipient = recipient;
        this.channel = channel;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public void setChannel(ChannelType channel) {
        this.channel = channel;
    }

    public Map<String, String> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, String> payload) {
        this.payload = payload;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
