package com.ecommerce.productservice.domain.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode; // Hibernate 6
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity // <--- This marks it as a Database Table
@Table(name = "outbox_events", indexes = {
        // Index helps the Poller find unprocessed events fast
        @Index(name = "idx_outbox_unprocessed", columnList = "processed, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    private String id;

    @Column(nullable = false)
    private String aggregateType; // e.g., "PRODUCT"
    @Column(nullable = false)
    private String aggregateId;   // e.g., "105" (Product ID)

    @Column(nullable = false)
    private String type;          // e.g., "PRODUCT_CREATED"
    // Stores the actual event data (JSON) so Kafka can read it later
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private String payload;
    private LocalDateTime createdAt;

    // False = Waiting to be sent to Kafka
    // True = Already sent
    private boolean processed;
}