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
    private String eventType;
    private ChannelType channelType;
    private String recipient;
    private Map<String, String> params;
}