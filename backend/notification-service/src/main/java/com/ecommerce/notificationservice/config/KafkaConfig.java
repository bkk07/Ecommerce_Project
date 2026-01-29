package com.ecommerce.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
@Configuration
public class KafkaConfig {
    // Factory for Urgent (OTP) - High Concurrency

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> urgentFactory(ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(5); // 5 Threads
        return factory;
    }

    // Factory for Transactional (Orders) - Medium Concurrency
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> transactionalFactory(ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(2); // 2 Threads
        return factory;
    }
    // Factory for Marketing - Low Concurrency
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> marketingFactory(ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1); // 1 Thread
        return factory;
    }
}