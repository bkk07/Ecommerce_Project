package com.ecommerce.orderservice.service;

import com.ecommerce.inventory.InventoryReleasedEvent;
import com.ecommerce.order.OrderNotificationType;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.SagaState;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.kafka.OrderEventPublisher;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.repository.SagaStateRepository;
import com.ecommerce.payment.PaymentRefundedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SagaOrchestratorService Unit Tests")
class SagaOrchestratorServiceTest {

    @Mock
    private SagaStateRepository sagaStateRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private SagaOrchestratorService sagaOrchestratorService;

    @Captor
    private ArgumentCaptor<SagaState> sagaStateCaptor;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private Order testOrder;
    private SagaState testSagaState;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .orderId("saga-order-123")
                .userId("user-789")
                .status(OrderStatus.CANCEL_REQUESTED)
                .totalAmount(new BigDecimal("499.99"))
                .items(Collections.emptyList())
                .build();

        testSagaState = SagaState.builder()
                .orderId("saga-order-123")
                .inventoryReleased(false)
                .paymentRefunded(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("handleInventoryReleased Tests")
    class HandleInventoryReleasedTests {

        @Test
        @DisplayName("Should update saga state when inventory is released")
        void shouldUpdateSagaStateWhenInventoryReleased() {
            // Given
            InventoryReleasedEvent event = new InventoryReleasedEvent();
            event.setOrderId("saga-order-123");

            when(sagaStateRepository.findById("saga-order-123")).thenReturn(Optional.of(testSagaState));
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(testSagaState);

            // When
            sagaOrchestratorService.handleInventoryReleased(event);

            // Then
            verify(sagaStateRepository).save(sagaStateCaptor.capture());
            assertThat(sagaStateCaptor.getValue().isInventoryReleased()).isTrue();
        }

        @Test
        @DisplayName("Should create new saga state if not exists")
        void shouldCreateNewSagaStateIfNotExists() {
            // Given
            InventoryReleasedEvent event = new InventoryReleasedEvent();
            event.setOrderId("new-saga-order");

            when(sagaStateRepository.findById("new-saga-order")).thenReturn(Optional.empty());
            when(sagaStateRepository.save(any(SagaState.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            sagaOrchestratorService.handleInventoryReleased(event);

            // Then
            verify(sagaStateRepository, times(2)).save(any(SagaState.class));
        }

        @Test
        @DisplayName("Should finalize cancellation when both inventory released and payment refunded")
        void shouldFinalizeCancellationWhenBothComplete() {
            // Given
            InventoryReleasedEvent event = new InventoryReleasedEvent();
            event.setOrderId("saga-order-123");

            testSagaState.setPaymentRefunded(true); // Payment already refunded
            testSagaState.setInventoryReleased(true); // After update

            when(sagaStateRepository.findById("saga-order-123")).thenReturn(Optional.of(testSagaState));
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(testSagaState);
            when(orderRepository.findByOrderId("saga-order-123")).thenReturn(Optional.of(testOrder));

            // When
            sagaOrchestratorService.handleInventoryReleased(event);

            // Then
            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderService).saveOutboxEvent(any(Order.class), eq(OrderNotificationType.ORDER_CANCELLED), anyString());
        }
    }

    @Nested
    @DisplayName("handlePaymentRefunded Tests")
    class HandlePaymentRefundedTests {

        @Test
        @DisplayName("Should update saga state when payment is refunded")
        void shouldUpdateSagaStateWhenPaymentRefunded() {
            // Given
            PaymentRefundedEvent event = new PaymentRefundedEvent();
            event.setOrderId("saga-order-123");
            event.setPaymentId("pay_refund_123");

            when(sagaStateRepository.findById("saga-order-123")).thenReturn(Optional.of(testSagaState));
            when(sagaStateRepository.save(any(SagaState.class))).thenReturn(testSagaState);
            when(orderRepository.findByOrderId("saga-order-123")).thenReturn(Optional.of(testOrder));

            // When
            sagaOrchestratorService.handlePaymentRefunded(event);

            // Then
            verify(sagaStateRepository).save(sagaStateCaptor.capture());
            assertThat(sagaStateCaptor.getValue().isPaymentRefunded()).isTrue();
            verify(orderService).saveOutboxEvent(any(Order.class), eq(OrderNotificationType.ORDER_REFUNDED), anyString());
        }
    }

    @Nested
    @DisplayName("retryStuckSagas Tests")
    class RetryStuckSagasTests {

        @Test
        @DisplayName("Should retry stuck sagas")
        void shouldRetryStuckSagas() {
            // Given
            testSagaState.setUpdatedAt(LocalDateTime.now().minusMinutes(10)); // Stuck for 10 minutes
            when(sagaStateRepository.findStuckSagas(any(LocalDateTime.class))).thenReturn(List.of(testSagaState));
            when(orderRepository.findByOrderId("saga-order-123")).thenReturn(Optional.of(testOrder));
            when(orderMapper.mapToOrderItemDtos(any())).thenReturn(Collections.emptyList());

            // When
            sagaOrchestratorService.retryStuckSagas();

            // Then
            verify(orderEventPublisher).handleOrderCancel(any());
            verify(sagaStateRepository).save(any(SagaState.class));
        }

        @Test
        @DisplayName("Should not retry if no stuck sagas")
        void shouldNotRetryIfNoStuckSagas() {
            // Given
            when(sagaStateRepository.findStuckSagas(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

            // When
            sagaOrchestratorService.retryStuckSagas();

            // Then
            verify(orderEventPublisher, never()).handleOrderCancel(any());
        }

        @Test
        @DisplayName("Should skip if order status is not CANCEL_REQUESTED")
        void shouldSkipIfOrderStatusNotCancelRequested() {
            // Given
            testSagaState.setUpdatedAt(LocalDateTime.now().minusMinutes(10));
            testOrder.setStatus(OrderStatus.PLACED); // Not CANCEL_REQUESTED
            
            when(sagaStateRepository.findStuckSagas(any(LocalDateTime.class))).thenReturn(List.of(testSagaState));
            when(orderRepository.findByOrderId("saga-order-123")).thenReturn(Optional.of(testOrder));

            // When
            sagaOrchestratorService.retryStuckSagas();

            // Then
            verify(orderEventPublisher, never()).handleOrderCancel(any());
        }
    }
}
