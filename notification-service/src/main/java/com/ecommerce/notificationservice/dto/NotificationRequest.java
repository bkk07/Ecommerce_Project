package com.ecommerce.notificationservice.dto;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequest {
    private String eventType;       // e.g., "ORDER_CREATED", "OTP_REQUEST"
    private ChannelType channelType; // e.g., "EMAIL", "SMS"
    private String recipient;       // email address or phone number
    private Map<String, String> params; // e.g., {"name": "John", "otp": "1234"}
}