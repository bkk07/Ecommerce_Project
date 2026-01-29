package com.ecommerce.order;

import java.time.Instant;

public class OrderNotificationEvent {
    private String eventId;
    private OrderNotificationType type;
    private String userId;
    private Instant timestamp;
    private int version;
    private OrderPayload payload;

    public OrderNotificationEvent() {
    }

    public OrderNotificationEvent(String eventId, OrderNotificationType type, String userId, Instant timestamp, int version, OrderPayload payload) {
        this.eventId = eventId;
        this.type = type;
        this.userId = userId;
        this.timestamp = timestamp;
        this.version = version;
        this.payload = payload;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public OrderNotificationType getType() {
        return type;
    }

    public void setType(OrderNotificationType type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public OrderPayload getPayload() {
        return payload;
    }

    public void setPayload(OrderPayload payload) {
        this.payload = payload;
    }
}
