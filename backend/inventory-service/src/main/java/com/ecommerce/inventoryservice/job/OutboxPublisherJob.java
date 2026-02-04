package com.ecommerce.inventoryservice.job;

import com.ecommerce.inventoryservice.model.OutboxEvent;
import com.ecommerce.inventoryservice.repository.OutboxEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Scheduled job to publish outbox events to Kafka.
 * Implements the Transactional Outbox Pattern for reliable event delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherJob {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${outbox.batch-size:100}")
    private int batchSize;

    @Value("${outbox.cleanup.retention-days:7}")
    private int retentionDays;

    /**
     * Publishes pending outbox events to Kafka in batches.
     * Runs every 2 seconds with pagination to prevent memory issues.
     */
    @Scheduled(fixedDelayString = "${outbox.publish-interval-ms:2000}")
    @Transactional
    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "publishEventsFallback")
    public void publishEvents() {
        List<OutboxEvent> events = outboxEventRepository
                .findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, batchSize));
        
        if (events.isEmpty()) {
            return;
        }
        
        log.debug("Processing {} outbox events", events.size());
        int successCount = 0;
        int failureCount = 0;
        
        for (OutboxEvent event : events) {
            try {
                publishSingleEvent(event);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to publish event id={}, type={}: {}", 
                        event.getId(), event.getEventType(), e.getMessage());
                // Continue with next event - this one will be retried next cycle
            }
        }
        
        if (successCount > 0 || failureCount > 0) {
            log.info("Outbox batch complete: {} published, {} failed", successCount, failureCount);
        }
    }

    /**
     * Publishes a single event to Kafka with retry support.
     */
    @Retry(name = "kafkaProducer")
    private void publishSingleEvent(OutboxEvent event) {
        log.debug("Publishing Outbox Event: {} to Topic: {}", event.getEventType(), event.getTopic());
        
        CompletableFuture<?> future = kafkaTemplate.send(
                event.getTopic(), 
                event.getAggregateId(), 
                event.getPayload()
        );
        
        // Wait for send confirmation (sync for reliability)
        future.join();
        
        // Mark as processed only after successful send
        event.setProcessed(true);
        outboxEventRepository.save(event);
    }

    /**
     * Fallback when Kafka circuit breaker is open.
     */
    public void publishEventsFallback(Exception e) {
        log.warn("Outbox publishing skipped - Kafka circuit breaker open: {}", e.getMessage());
    }

    /**
     * Cleanup old processed events to prevent table bloat.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "${outbox.cleanup.cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupProcessedEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        int deletedCount = outboxEventRepository.deleteProcessedEventsBefore(cutoffDate);
        
        if (deletedCount > 0) {
            log.info("Outbox cleanup: Deleted {} processed events older than {} days", 
                    deletedCount, retentionDays);
        }
    }
}
