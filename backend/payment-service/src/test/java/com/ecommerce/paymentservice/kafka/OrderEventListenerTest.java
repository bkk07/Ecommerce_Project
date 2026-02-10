package com.ecommerce.paymentservice.kafka;

import com.ecommerce.order.OrderCreatedEvent;
import com.ecommerce.payment.PaymentInitiatedEvent;
import com.ecommerce.paymentservice.exception.PaymentAlreadyExistsException;
import com.ecommerce.paymentservice.exception.PaymentGatewayException;
import com.ecommerce.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderEventListener
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventListener Unit Tests")
class OrderEventListenerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Nested
    @DisplayName("handleOrderCreated Tests")
    class HandleOrderCreatedTests {

        @Test
        @DisplayName("Should process order created event and publish payment initiated event")
        void handleOrderCreated_Success() {
            // Given
            OrderCreatedEvent event = createOrderCreatedEvent();
            PaymentInitiatedEvent paymentEvent = new PaymentInitiatedEvent(
                    event.getOrderId(),
                    "order_razorpay123",
                    event.getTotalAmount(),
                    event.getUserId());

            when(paymentService.handleOrderCreated(any(OrderCreatedEvent.class)))
                    .thenReturn(paymentEvent);

            // When
            orderEventListener.handleOrderCreated(event);

            // Then
            verify(paymentService).handleOrderCreated(event);
            
            ArgumentCaptor<PaymentInitiatedEvent> captor = 
                    ArgumentCaptor.forClass(PaymentInitiatedEvent.class);
            verify(paymentEventProducer).publishPaymentInitiated(captor.capture());
            
            PaymentInitiatedEvent capturedEvent = captor.getValue();
            assertThat(capturedEvent.getOrderId()).isEqualTo(event.getOrderId());
            assertThat(capturedEvent.getRazorpayOrderId()).isEqualTo("order_razorpay123");
        }

        @Test
        @DisplayName("Should skip publishing when payment already exists")
        void handleOrderCreated_PaymentExists_SkipsPublishing() {
            // Given
            OrderCreatedEvent event = createOrderCreatedEvent();
            when(paymentService.handleOrderCreated(any(OrderCreatedEvent.class)))
                    .thenThrow(PaymentAlreadyExistsException.forOrderId(event.getOrderId()));

            // When
            orderEventListener.handleOrderCreated(event);

            // Then
            verify(paymentService).handleOrderCreated(event);
            verify(paymentEventProducer, never()).publishPaymentInitiated(any());
        }

        @Test
        @DisplayName("Should handle gateway exception gracefully")
        void handleOrderCreated_GatewayException_HandlesGracefully() {
            // Given
            OrderCreatedEvent event = createOrderCreatedEvent();
            when(paymentService.handleOrderCreated(any(OrderCreatedEvent.class)))
                    .thenThrow(new PaymentGatewayException("Gateway unavailable"));

            // When
            orderEventListener.handleOrderCreated(event);

            // Then
            verify(paymentService).handleOrderCreated(event);
            verify(paymentEventProducer, never()).publishPaymentInitiated(any());
        }

        @Test
        @DisplayName("Should handle unexpected exception gracefully")
        void handleOrderCreated_UnexpectedException_HandlesGracefully() {
            // Given
            OrderCreatedEvent event = createOrderCreatedEvent();
            when(paymentService.handleOrderCreated(any(OrderCreatedEvent.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // When
            orderEventListener.handleOrderCreated(event);

            // Then
            verify(paymentService).handleOrderCreated(event);
            verify(paymentEventProducer, never()).publishPaymentInitiated(any());
        }
    }

    private OrderCreatedEvent createOrderCreatedEvent() {
        return new OrderCreatedEvent(
                "order123",
                "user456",
                new BigDecimal("1500.00"),
                null,
                null);
    }
}
