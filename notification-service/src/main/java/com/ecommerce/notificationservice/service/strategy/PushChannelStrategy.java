package com.ecommerce.notificationservice.service.strategy;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import com.ecommerce.notificationservice.exception.VendorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PushChannelStrategy implements NotificationChannelStrategy {

    @Override
    public void send(String recipient,String subject,String message) {
        log.info(">>> Connecting to Push Notification Provider (FCM/Apple APNs)...");
        // Simulate Logic
        try {
            // Mocking a network call
            Thread.sleep(50);
            log.info("[PUSH SENT] Token: {}, Payload: {}", recipient, message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VendorException("Push Notification Service Interrupted");
        }
    }

    @Override
    public ChannelType getSupportedType() {
        return ChannelType.PUSH;
    }
}