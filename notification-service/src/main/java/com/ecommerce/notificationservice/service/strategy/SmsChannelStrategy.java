package com.ecommerce.notificationservice.service.strategy;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import com.ecommerce.notificationservice.exception.VendorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsChannelStrategy implements NotificationChannelStrategy {

    @Override
    public void send(String recipient, String message) {
        log.info(">>> Connecting to SMS Provider (Twilio/AWS SNS)...");
        // Simulate Logic
        if (recipient == null || recipient.isEmpty()) {
            throw new VendorException("Invalid phone number");
        }
        log.info("[SMS SENT] To: {}, Message: {}", recipient, message);
    }
    @Override
    public ChannelType getSupportedType() {
        return ChannelType.SMS;
    }
}