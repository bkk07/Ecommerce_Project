package com.ecommerce.event;
public class UserEvent {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String eventType; // CREATED, UPDATED

    public UserEvent() {
    }
    public UserEvent(Long userId, String name, String email, String phone, String eventType) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.eventType = eventType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}
