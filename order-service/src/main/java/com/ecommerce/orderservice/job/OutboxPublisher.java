package com.ecommerce.orderservice.job;

import com.ecommerce.order.OrderNotificationEvent;
import com.ecommerce.orderservice.entity.OrderOutbox;
import com.ecommerce.orderservice.entity.OutboxStatus;
import com.ecommerce.orderservice.repository.OrderOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ecommerce.common.KafkaProperties.ORDER_NOTIFICATIONS_TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OrderOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    @Transactional
    public void publishOutboxEvents() {
        List<OrderOutbox> pendingEvents = outboxRepository.findByStatus(OutboxStatus.PENDING);

        for (OrderOutbox outbox : pendingEvents) {
            try {
                OrderNotificationEvent event = objectMapper.readValue(outbox.getPayload(), OrderNotificationEvent.class);
                
                kafkaTemplate.send(ORDER_NOTIFICATIONS_TOPIC, event.getEventId(), event)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                outbox.setStatus(OutboxStatus.SENT);
                                outboxRepository.save(outbox);
                                log.info("Published outbox event: {}", outbox.getEventId());
                            } else {
                                log.error("Failed to publish outbox event: {}", outbox.getEventId(), ex);
                            }
                        });

            } catch (Exception e) {
                log.error("Error processing outbox event: {}", outbox.getEventId(), e);
            }
        }
    }
}
