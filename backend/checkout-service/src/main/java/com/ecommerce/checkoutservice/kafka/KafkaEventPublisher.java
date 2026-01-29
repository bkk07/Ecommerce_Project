package com.ecommerce.checkoutservice.kafka;

import com.ecommerce.checkout.CreateOrderCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.ecommerce.common.KafkaProperties.CREATE_ORDER_COMMAND_TOPIC;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {
    private final KafkaTemplate<String, CreateOrderCommand> kafkaTemplate;
    public void publishCreateOrderCommand(CreateOrderCommand command) {
        log.info("Publishing Create Order Command for user: {}", command.getUserId());
        kafkaTemplate.send(CREATE_ORDER_COMMAND_TOPIC, command.getUserId(), command);
        log.info("Published Create Order Command: {}", command);
    }
}
