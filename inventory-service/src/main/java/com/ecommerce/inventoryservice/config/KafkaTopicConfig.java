package com.ecommerce.inventoryservice.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import static com.ecommerce.common.KafkaProperties.INVENTORY_EVENTS_TOPIC;
@Slf4j
@Configuration
public class KafkaTopicConfig {
    // Topic for sending updates TO Search Service
    @Bean
    public NewTopic inventoryUpdatedTopic() {
        return TopicBuilder.name(INVENTORY_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
    /**
     * Configures the Error Handler for Kafka Consumers.
     * 1. Retries 3 times with a 1-second delay.
     * 2. If it still fails, sends the message to a topic named 'original-topic.DLT'.
     */
    @Bean
    public CommonErrorHandler errorHandler(KafkaOperations<Object, Object> template) {

        // Strategy: Send failed messages to a Dead Letter Topic (DLT)
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);

        // Retry Strategy: 3 attempts, 1000ms apart
        FixedBackOff backOff = new FixedBackOff(1000L, 3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Optional: Log exceptions
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            if(ex != null)
                log.error("Failed to consume message. Attempt: {}. Error: {}", deliveryAttempt, ex.getMessage());
        });
        return errorHandler;
    }
}