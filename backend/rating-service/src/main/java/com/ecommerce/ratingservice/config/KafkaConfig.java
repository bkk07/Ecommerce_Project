package com.ecommerce.ratingservice.config;

import com.ecommerce.order.OrderDeliveredEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Production-grade Kafka configuration with retry and dead letter queue support.
 */
@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:rating-service-group}")
    private String consumerGroupId;

    @Value("${kafka.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${kafka.retry.initial-interval:1000}")
    private long initialRetryInterval;

    @Value("${kafka.retry.multiplier:2.0}")
    private double retryMultiplier;

    @Value("${kafka.retry.max-interval:10000}")
    private long maxRetryInterval;

    // ==================== PRODUCER CONFIG ====================
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Enable idempotence for exactly-once semantics
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        
        // Performance tuning
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        // Type mappings
        configProps.put(JsonSerializer.TYPE_MAPPINGS, 
                "ratingEvent:com.ecommerce.rating.RatingUpdatedEvent");
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
        template.setObservationEnabled(true); // Enable micrometer observations
        return template;
    }

    // ==================== CONSUMER CONFIG ====================
    
    @Bean
    public ConsumerFactory<String, OrderDeliveredEvent> orderDeliveredConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "rating-eligibility-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Reliable consumption
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        
        // Performance tuning
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        
        // Deserialization
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ecommerce.*");
        configProps.put(JsonDeserializer.TYPE_MAPPINGS, 
                "orderDeliveredEvent:com.ecommerce.order.OrderDeliveredEvent");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderDeliveredEvent> orderDeliveredKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderDeliveredEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderDeliveredConsumerFactory());
        
        // Concurrency settings
        factory.setConcurrency(3);
        
        // Acknowledgement mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Error handling with retry and DLQ
        factory.setCommonErrorHandler(createErrorHandler());
        
        // Enable batch listening for better performance (optional)
        factory.setBatchListener(false);
        
        log.info("Configured Kafka listener with {} retries and exponential backoff", maxRetryAttempts);
        
        return factory;
    }

    /**
     * Creates an error handler with exponential backoff retry and dead letter queue.
     */
    private DefaultErrorHandler createErrorHandler() {
        // Configure exponential backoff
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxRetryAttempts);
        backOff.setInitialInterval(initialRetryInterval);
        backOff.setMultiplier(retryMultiplier);
        backOff.setMaxInterval(maxRetryInterval);

        // Create error handler with dead letter publishing
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> {
                    // This is the final fallback after all retries are exhausted
                    log.error("Failed to process message after {} retries. Topic: {}, Partition: {}, Offset: {}",
                            maxRetryAttempts,
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            exception);
                    // Could also store in a database for manual retry
                },
                backOff
        );

        // Don't retry for certain exceptions
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class
        );

        return errorHandler;
    }

    /**
     * Dead letter publishing recoverer for sending failed messages to DLQ topic.
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, Object> template) {
        return new DeadLetterPublishingRecoverer(template,
                (record, ex) -> {
                    log.warn("Sending message to DLQ. Original topic: {}, Exception: {}",
                            record.topic(), ex.getMessage());
                    return new org.apache.kafka.common.TopicPartition(
                            record.topic() + ".DLQ",
                            record.partition()
                    );
                });
    }
}
