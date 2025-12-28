package com.ecommerce.notificationservice.infrastructure.entity;

import com.ecommerce.notificationservice.domain.enumtype.ChannelType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationTemplateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String eventType;

    @Enumerated(EnumType.STRING)
    private ChannelType channelType;
    private String subject;
    private String bodyTemplate;
}
