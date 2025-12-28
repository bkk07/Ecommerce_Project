package com.ecommerce.notificationservice.domain.model;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import com.ecommerce.notificationservice.domain.enumtype.NotificationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationLog {
    private Long id;
    private String recipient;
    private String content;
    private ChannelType channelType;
    private NotificationStatus status;
    private int retryCount;
    private String errorMessage;
    private LocalDateTime createdAt;
}