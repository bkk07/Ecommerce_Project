package com.ecommerce.notificationservice.exception;

public class TemplateNotFoundException extends NotificationServiceException {
    public TemplateNotFoundException(String message) {
        super(message);
    }
}