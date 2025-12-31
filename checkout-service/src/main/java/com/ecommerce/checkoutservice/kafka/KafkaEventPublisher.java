package com.ecommerce.checkoutservice.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void publishOrderEvent(OrderPlacedEvent event) {
        // Send message to "order-placed" topic
        kafkaTemplate.send("order-placed", event.getUserId(), event);
    }
}
