package com.ecommerce.orderservice.kafka;

import com.ecommerce.checkout.CreateOrderCommand;
import com.ecommerce.inventory.InventoryLockFailedEvent;
import com.ecommerce.inventory.InventoryReleasedEvent;
import com.ecommerce.order.OrderItemDto;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.SagaState;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.feign.PaymentFeign;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.repository.SagaStateRepository;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.orderservice.service.SagaOrchestratorService;
import com.ecommerce.payment.PaymentInitiatedEvent;
import com.ecommerce.payment.PaymentRefundedEvent;
import com.ecommerce.payment.PaymentSuccessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("OrderEventListener Integration Tests")
class OrderEventListenerIntegrationTest {

    @Autowired
    private OrderEventListener orderEventListener;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SagaOrchestratorService sagaOrchestratorService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SagaStateRepository sagaStateRepository;

    @MockitoBean
    private PaymentFeign paymentFeign;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        sagaStateRepository.deleteAll();

        testOrder = Order.builder()
                .orderId("kafka-test-order")
                .userId("kafka-user-123")
                .status(OrderStatus.PAYMENT_READY)
                .totalAmount(new BigDecimal("599.99"))
                .shippingAddress("Kafka Street 123")
                .createdAt(LocalDateTime.now())
                .build();

        OrderItem item = OrderItem.builder()
                .skuCode("KAFKA-SKU-001")
                .productName("Kafka Test Product")
                .price(new BigDecimal("599.99"))
                .quantity(1)
                .order(testOrder)
                .build();

        testOrder.setItems(List.of(item));
        orderRepository.save(testOrder);
    }

    @Nested
    @DisplayName("handlePaymentSuccess Tests")
    class HandlePaymentSuccessTests {

        @Test
        @DisplayName("Should update order to PLACED on payment success")
        void shouldUpdateOrderToPlacedOnPaymentSuccess() {
            // Given
            PaymentSuccessEvent event = new PaymentSuccessEvent();
            event.setOrderId("kafka-test-order");
            event.setPaymentId("pay_success_123");

            // When
            orderEventListener.handlePaymentSuccess(event);

            // Then
            Order updatedOrder = orderRepository.findByOrderId("kafka-test-order").orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
            assertThat(updatedOrder.getPaymentId()).isEqualTo("pay_success_123");
        }

        @Test
        @DisplayName("Should be idempotent - skip if already PLACED")
        void shouldSkipIfAlreadyPlaced() {
            // Given
            testOrder.setStatus(OrderStatus.PLACED);
            testOrder.setPaymentId("existing_pay_123");
            orderRepository.save(testOrder);

            PaymentSuccessEvent event = new PaymentSuccessEvent();
            event.setOrderId("kafka-test-order");
            event.setPaymentId("new_pay_456");

            // When
            orderEventListener.handlePaymentSuccess(event);

            // Then
            Order order = orderRepository.findByOrderId("kafka-test-order").orElseThrow();
            assertThat(order.getPaymentId()).isEqualTo("existing_pay_123"); // Unchanged
        }
    }

    @Nested
    @DisplayName("handlePaymentInitiated Tests")
    class HandlePaymentInitiatedTests {

        @Test
        @DisplayName("Should update order to PAYMENT_READY")
        void shouldUpdateOrderToPaymentReady() {
            // Given
            testOrder.setStatus(OrderStatus.PENDING);
            orderRepository.save(testOrder);

            PaymentInitiatedEvent event = new PaymentInitiatedEvent();
            event.setOrderId("kafka-test-order");
            event.setRazorpayOrderId("razorpay_initiated_123");

            // When
            orderEventListener.handlePaymentInitiated(event);

            // Then
            Order updatedOrder = orderRepository.findByOrderId("kafka-test-order").orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_READY);
            assertThat(updatedOrder.getRazorpayOrderId()).isEqualTo("razorpay_initiated_123");
        }
    }

    @Nested
    @DisplayName("handleInventoryLockFailed Tests")
    class HandleInventoryLockFailedTests {

        @Test
        @DisplayName("Should cancel order on inventory lock failure")
        void shouldCancelOrderOnInventoryLockFailure() {
            // Given
            InventoryLockFailedEvent event = new InventoryLockFailedEvent();
            event.setOrderId("kafka-test-order");

            // When
            orderEventListener.handleInventoryLockFailed(event);

            // Then
            Order cancelledOrder = orderRepository.findByOrderId("kafka-test-order").orElseThrow();
            assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("handleInventoryReleased Tests")
    class HandleInventoryReleasedTests {

        @Test
        @DisplayName("Should update saga state when inventory released")
        void shouldUpdateSagaStateWhenInventoryReleased() {
            // Given
            testOrder.setStatus(OrderStatus.CANCEL_REQUESTED);
            orderRepository.save(testOrder);

            SagaState sagaState = SagaState.builder()
                    .orderId("kafka-test-order")
                    .inventoryReleased(false)
                    .paymentRefunded(false)
                    .build();
            sagaStateRepository.save(sagaState);

            InventoryReleasedEvent event = new InventoryReleasedEvent();
            event.setOrderId("kafka-test-order");

            // When
            orderEventListener.handleInventoryReleased(event);

            // Then
            SagaState updatedState = sagaStateRepository.findById("kafka-test-order").orElseThrow();
            assertThat(updatedState.isInventoryReleased()).isTrue();
        }
    }

    @Nested
    @DisplayName("handlePaymentRefunded Tests")
    class HandlePaymentRefundedTests {

        @Test
        @DisplayName("Should update saga state and finalize cancellation")
        void shouldUpdateSagaStateAndFinalizeCancellation() {
            // Given
            testOrder.setStatus(OrderStatus.CANCEL_REQUESTED);
            orderRepository.save(testOrder);

            SagaState sagaState = SagaState.builder()
                    .orderId("kafka-test-order")
                    .inventoryReleased(true) // Already released
                    .paymentRefunded(false)
                    .build();
            sagaStateRepository.save(sagaState);

            PaymentRefundedEvent event = new PaymentRefundedEvent();
            event.setOrderId("kafka-test-order");
            event.setPaymentId("pay_refund_123");

            // When
            orderEventListener.handlePaymentRefunded(event);

            // Then
            SagaState updatedState = sagaStateRepository.findById("kafka-test-order").orElseThrow();
            assertThat(updatedState.isPaymentRefunded()).isTrue();

            Order cancelledOrder = orderRepository.findByOrderId("kafka-test-order").orElseThrow();
            assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("handleCreateOrderCommand Tests")
    class HandleCreateOrderCommandTests {

        @Test
        @DisplayName("Should create order from command")
        void shouldCreateOrderFromCommand() {
            // Given
            OrderItemDto itemDto = new OrderItemDto();
            itemDto.setSkuCode("CMD-SKU-001");
            itemDto.setProductName("Command Product");
            itemDto.setPrice(new BigDecimal("99.99"));
            itemDto.setQuantity(1);

            CreateOrderCommand command = new CreateOrderCommand();
            command.setUserId("command-user-456");
            command.setTotalAmount(new BigDecimal("99.99"));
            command.setShippingAddress("Command Street 789");
            command.setItems(List.of(itemDto));

            PaymentInitiatedEvent paymentEvent = new PaymentInitiatedEvent();
            paymentEvent.setRazorpayOrderId("razorpay_cmd_123");
            paymentEvent.setOrderId("cmd-order-id");

            when(paymentFeign.createPayment(any())).thenReturn(paymentEvent);

            // When
            orderEventListener.handleCreateOrderCommand(command);

            // Then
            List<Order> userOrders = orderRepository.findByUserId("command-user-456");
            assertThat(userOrders).hasSize(1);
            assertThat(userOrders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(userOrders.get(0).getTotalAmount()).isEqualByComparingTo(new BigDecimal("99.99"));

            verify(orderEventPublisher).publishInventoryLockEvent(any());
        }
    }
}
