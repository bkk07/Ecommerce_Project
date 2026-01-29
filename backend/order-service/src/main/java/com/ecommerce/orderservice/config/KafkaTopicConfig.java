package com.ecommerce.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import static com.ecommerce.common.KafkaProperties.ORDER_NOTIFICATIONS_TOPIC;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderNotificationsTopic() {
        return TopicBuilder.name(ORDER_NOTIFICATIONS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
