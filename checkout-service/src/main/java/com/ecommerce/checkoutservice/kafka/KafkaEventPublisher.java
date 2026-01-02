package com.ecommerce.checkoutservice.kafka;

import com.ecommerce.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.ecommerce.common.KafkaProperties.ORDER_CREATED_EVENTS_TOPIC;
import static com.ecommerce.common.KafkaProperties.ORDER_EVENTS_TOPIC;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    public void publishOrderEvent(OrderCreatedEvent event) {
        log.info("Publishing Order Event: {}", event);
        kafkaTemplate.send(ORDER_CREATED_EVENTS_TOPIC, String.valueOf(event.getUserId()), event);
        log.info("Published Order Event: {}", event);
    }
}
