package com.ecommerce.notificationservice.exception;

public abstract class NotificationServiceException extends RuntimeException {
    public NotificationServiceException(String message) {
        super(message);
    }
}