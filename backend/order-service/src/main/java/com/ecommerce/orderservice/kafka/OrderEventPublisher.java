package com.ecommerce.orderservice.kafka;

import com.ecommerce.inventory.InventoryLockEvent;
import com.ecommerce.order.OrderCancelEvent;
import com.ecommerce.order.OrderCreatedEvent;
import com.ecommerce.order.OrderDeliveredEvent;
import com.ecommerce.order.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.ecommerce.common.KafkaProperties.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void  handleOrderCancel(OrderCancelEvent orderCancelEvent) {
        log.info("Publishing Order cancel to the notification service Order Id: {}",orderCancelEvent.getOrderId());
        kafkaTemplate.send(ORDER_CANCEL_EVENTS_TOPIC ,orderCancelEvent.getOrderId(),orderCancelEvent);
    }
    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for Order: {}", event.getOrderId());
        kafkaTemplate.send(ORDER_CREATED_EVENTS_TOPIC, event.getOrderId(), event);
    }

    public void publishInventoryLockEvent(InventoryLockEvent event) {
        log.info("Publishing InventoryLockEvent for Order: {}", event.getOrderId());
        kafkaTemplate.send(INVENTORY_LOCK_TOPIC, event.getOrderId(), event);
    }
    public void publishOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Publishing OrderPlacedEvent for Order: {}", event.getOrderId());
        kafkaTemplate.send(ORDER_PLACED_TOPIC, event.getOrderId(), event);
    }

    public void publishOrderDeliveredEvent(OrderDeliveredEvent event) {
        log.info("Publishing OrderDeliveredEvent for Order: {} with {} items", 
                event.getOrderId(), event.getItems() != null ? event.getItems().size() : 0);
        kafkaTemplate.send(ORDER_DELIVERED_TOPIC, event.getOrderId(), event);
    }
}
