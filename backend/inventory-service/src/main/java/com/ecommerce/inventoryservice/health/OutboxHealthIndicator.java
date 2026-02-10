package com.ecommerce.inventoryservice.health;

import com.ecommerce.inventoryservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for Outbox event processing.
 * Monitors the backlog of unprocessed events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxHealthIndicator implements HealthIndicator {

    private final OutboxEventRepository outboxEventRepository;
    private static final int WARNING_THRESHOLD = 100;
    private static final int CRITICAL_THRESHOLD = 1000;

    @Override
    public Health health() {
        try {
            long pendingCount = outboxEventRepository.countByProcessedFalse();
            
            Health.Builder healthBuilder;
            
            if (pendingCount >= CRITICAL_THRESHOLD) {
                healthBuilder = Health.down()
                        .withDetail("status", "CRITICAL - Large backlog detected");
            } else if (pendingCount >= WARNING_THRESHOLD) {
                healthBuilder = Health.status("WARNING")
                        .withDetail("status", "WARNING - Backlog building up");
            } else {
                healthBuilder = Health.up()
                        .withDetail("status", "OK");
            }
            
            return healthBuilder
                    .withDetail("pendingEvents", pendingCount)
                    .withDetail("warningThreshold", WARNING_THRESHOLD)
                    .withDetail("criticalThreshold", CRITICAL_THRESHOLD)
                    .build();
                    
        } catch (Exception e) {
            log.error("Outbox health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
