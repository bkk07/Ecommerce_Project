package com.ecommerce.userservice.api.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class  NotificationRequest {
    private String eventType;
    private String channelType;
    private String recipient;
    private Map<String, String> params;
}

