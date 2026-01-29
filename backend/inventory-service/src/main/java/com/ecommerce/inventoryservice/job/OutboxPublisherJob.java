package com.ecommerce.inventoryservice.job;

import com.ecommerce.inventoryservice.model.OutboxEvent;
import com.ecommerce.inventoryservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherJob {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 2000) // Run every 2 seconds
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByProcessedFalse();
        
        for (OutboxEvent event : events) {
            try {
                log.info("Publishing Outbox Event: {} to Topic: {}", event.getEventType(), event.getTopic());
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload());
                
                // Mark as processed (or delete)
                event.setProcessed(true);
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getId(), e);
                // Will retry next time
            }
        }
    }
}
