package com.ecommerce.userservice.config;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // Define Topics
    @Bean
    public NewTopic urgentTopic() {
        return TopicBuilder.name("notifications.urgent")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionalTopic() {
        return TopicBuilder.name("notifications.transactional")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
