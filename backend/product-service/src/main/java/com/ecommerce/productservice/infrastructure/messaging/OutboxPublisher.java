package com.ecommerce.productservice.infrastructure.messaging;

import com.ecommerce.productservice.domain.entity.OutboxEvent;
import com.ecommerce.productservice.domain.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    // Run every 500ms for higher responsiveness
    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishEvents() {

        // 1. Fetch unprocessed events (Oldest first)
        // Batch size of 50 prevents memory spikes and long transactions
        Pageable limit = PageRequest.of(0, 50);
        List<OutboxEvent> events = outboxRepo.findByProcessedFalseOrderByCreatedAtAsc(limit);

        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} unprocessed events. Publishing to Kafka...", events.size());

        for (OutboxEvent event : events) {
            try {
                // 2. Send to Kafka Synchronously
                // Blocking here ensures we only mark 'processed' if Kafka actually accepts the message.
                // This guarantees "At-Least-Once" delivery.
                kafkaTemplate.send(PRODUCT_EVENTS_TOPIC, event.getAggregateId(), event.getPayload()).get();

                // 3. Mark as Processed
                event.setProcessed(true);

            } catch (Exception e) {
                log.error("Failed to send event ID: {}", event.getId(), e);
                // If Kafka send fails, we do NOT mark as processed.
                // The event will be retried in the next polling cycle.
            }
        }

        // 4. Save updated status to DB
        // If this fails, the transaction rolls back, 'processed' remains false,
        // and we resend the message next time (Duplicate). Consumers must be idempotent.
        outboxRepo.saveAll(events);
    }
}
