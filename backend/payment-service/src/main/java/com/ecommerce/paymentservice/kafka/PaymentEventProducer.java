package com.ecommerce.paymentservice.kafka;
import com.ecommerce.payment.PaymentInitiatedEvent;
import com.ecommerce.payment.PaymentRefundedEvent;
import com.ecommerce.payment.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.ecommerce.common.KafkaProperties.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        kafkaTemplate.send(PAYMENTS_EVENTS_SUCCESS_TOPIC,event.getOrderId(),event);
    }

    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        log.info("Publishing PaymentInitiatedEvent for Order: {}", event.getOrderId());
        kafkaTemplate.send(PAYMENT_INITIATED_EVENT_TOPIC, event.getOrderId(), event);
    }

    public void publishPaymentRefunded(PaymentRefundedEvent event) {
        log.info("Publishing PaymentRefundedEvent for Order: {}", event.getOrderId());
        kafkaTemplate.send(PAYMENT_REFUNDED_EVENTS_TOPIC, event.getOrderId(), event);
    }
}
