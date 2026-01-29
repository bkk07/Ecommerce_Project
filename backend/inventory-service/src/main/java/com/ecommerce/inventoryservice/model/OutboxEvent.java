package com.ecommerce.inventoryservice.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateId; // e.g., OrderId or SkuCode

    @Column(nullable = false)
    private String eventType; // e.g., "InventoryReleasedEvent"

    @Lob
    @Column(nullable = false)
    private String payload; // JSON string of the event

    @Column(nullable = false)
    private String topic;

    private boolean processed;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
