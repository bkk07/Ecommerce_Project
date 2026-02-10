package com.ecommerce.inventoryservice.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Custom health indicator for Kafka connectivity.
 * Checks if the Kafka cluster is reachable and responsive.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;
    private static final int TIMEOUT_MS = 5000;

    @Override
    public Health health() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClusterOptions options = new DescribeClusterOptions()
                    .timeoutMs(TIMEOUT_MS);
            
            var clusterResult = adminClient.describeCluster(options);
            
            // Get cluster information with timeout
            String clusterId = clusterResult.clusterId().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            int nodeCount = clusterResult.nodes().get(TIMEOUT_MS, TimeUnit.MILLISECONDS).size();
            
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodeCount)
                    .withDetail("status", "Connected")
                    .build();
                    
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("status", "Disconnected")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
