package com.ecommerce.cartservice.consumer;

import com.ecommerce.cartservice.dto.OrderPlacedEvent;
import com.ecommerce.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static com.ecommerce.common.KafkaProperties.ORDER_EVENTS_TOPIC;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartConsumer {

    private final CartService cartService;
    @KafkaListener(topics = ORDER_EVENTS_TOPIC, groupId = "cart-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Order Placed Event received. Clearing cart for User: {}", event.getUserId());
        cartService.clearCart(event.getUserId());
    }
}