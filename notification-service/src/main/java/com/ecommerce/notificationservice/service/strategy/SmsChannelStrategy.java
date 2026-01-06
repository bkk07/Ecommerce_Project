package com.ecommerce.notificationservice.service.strategy;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import com.ecommerce.notificationservice.exception.VendorException;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsChannelStrategy implements NotificationChannelStrategy {

    @Value("${SID_TWILIO}")
    private String accountSid;

    @Value("${TOKEN_TWILIO}")
    private String authToken;

    @Value("${TOKEN_TWILIO}")
    private String fromPhoneNumber;

    // Initialize Twilio once at application startup
    @PostConstruct
    public void init() {
        try {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized successfully with Account SID: {}", accountSid);
        } catch (Exception e) {
            log.error("Failed to initialize Twilio. SMS sending will fail.", e);
        }
    }

    @Override
    public void send(String recipient, String subject, String messageBody) {
        // NOTE: SMS does not support 'Subject', so we simply ignore that parameter.
        log.info("Connecting to Twilio to send SMS to: {}", recipient);

        try {
            if (recipient == null || recipient.isEmpty()) {
                throw new VendorException("Recipient phone number cannot be empty", "Recipient phone number cannot be empty");
            }

            // Create and Send Message
            Message message = Message.creator(
                    new PhoneNumber(recipient),       // To
                    new PhoneNumber(fromPhoneNumber), // From (Twilio Number)
                    messageBody                       // Body
            ).create();

            log.info("[SMS SENT] SID: {}, Status: {}", message.getSid(), message.getStatus());

        } catch (ApiException e) {
            // Handle specific Twilio errors (like invalid number, insufficient funds)
            log.error("Twilio API Error: Code {}, Message {}", e.getCode(), e.getMessage());
            throw new VendorException("Twilio SMS failed: " + e.getMessage(), e.getMessage());
        } catch (Exception e) {
            log.error("Unknown SMS Error", e);
            throw new VendorException("SMS sending failed: " + e.getMessage(), e.getMessage());
        }
    }

    @Override
    public ChannelType getSupportedType() {
        return ChannelType.SMS;
    }
}