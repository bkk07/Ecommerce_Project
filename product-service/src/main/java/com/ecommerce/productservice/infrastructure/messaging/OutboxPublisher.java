package com.ecommerce.productservice.infrastructure.messaging;

import com.ecommerce.productservice.domain.entity.OutboxEvent;
import com.ecommerce.productservice.domain.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ecommerce.common.KafkaProperties.PRODUCT_EVENTS_TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;
    // Run every 5 seconds (Adjust to 500ms for high-speed systems)
    @Scheduled(fixedDelay = 5000)
    @Transactional // Ensure DB update happens only if Kafka send succeeds
    public void publishEvents() {

        // 1. Fetch unprocessed events (Oldest first)
        // You might want to limit this to 50 at a time to avoid memory spikes
        List<OutboxEvent> events = outboxRepo.findByProcessedFalseOrderByCreatedAtAsc();

        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} unprocessed events. Publishing to Kafka...", events.size());

        for (OutboxEvent event : events) {
            try {
                // 2. Send to Kafka
                // Key = AggregateID (Product ID) -> Ensures ordering for that product
                kafkaTemplate.send(PRODUCT_EVENTS_TOPIC, event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to send event ID: {}", event.getId(), ex);
                                // Logic to handle failure (e.g., retry count) could go here
                            }
                        });

                // 3. Mark as Processed immediately (Optimistic approach)
                // In a stricter system, you'd wait for the future callback,
                // but inside @Transactional, this works well for simple cases.
                event.setProcessed(true);

            } catch (Exception e) {
                log.error("Error processing outbox event: {}", event.getId(), e);
            }
        }

        // 4. Save updated status to DB
        outboxRepo.saveAll(events);
    }
}