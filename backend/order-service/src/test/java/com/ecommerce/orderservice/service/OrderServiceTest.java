package com.ecommerce.orderservice.service;

import com.ecommerce.checkout.CreateOrderCommand;
import com.ecommerce.checkout.OrderCheckoutResponse;
import com.ecommerce.order.OrderCreatedEvent;
import com.ecommerce.order.OrderItemDto;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.exception.OrderCancellationException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.feign.PaymentFeign;
import com.ecommerce.orderservice.kafka.OrderEventPublisher;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderOutboxRepository;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.payment.PaymentInitiatedEvent;
import com.ecommerce.payment.PaymentSuccessEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OrderMapper orderMapper;
    
    @Mock
    private OrderEventPublisher orderEventPublisher;
    
    @Mock
    private OrderOutboxRepository orderOutboxRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private PaymentFeign paymentFeign;
    
    @InjectMocks
    private OrderService orderService;
    
    @Captor
    private ArgumentCaptor<Order> orderCaptor;
    
    private Order testOrder;
    private OrderResponse testOrderResponse;
    private CreateOrderCommand testCreateOrderCommand;
    
    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .orderId("test-order-123")
                .userId("user-456")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("199.99"))
                .shippingAddress("123 Test Street")
                .createdAt(LocalDateTime.now())
                .items(List.of(
                        OrderItem.builder()
                                .skuCode("SKU-001")
                                .productName("Test Product")
                                .price(new BigDecimal("199.99"))
                                .quantity(1)
                                .build()
                ))
                .build();
        
        testOrderResponse = OrderResponse.builder()
                .orderNumber("test-order-123")
                .status("PENDING")
                .totalAmount(new BigDecimal("199.99"))
                .shippingAddress("123 Test Street")
                .build();
        
        OrderItemDto itemDto = new OrderItemDto();
        itemDto.setSkuCode("SKU-001");
        itemDto.setProductName("Test Product");
        itemDto.setPrice(new BigDecimal("199.99"));
        itemDto.setQuantity(1);
        
        testCreateOrderCommand = new CreateOrderCommand();
        testCreateOrderCommand.setUserId("user-456");
        testCreateOrderCommand.setTotalAmount(new BigDecimal("199.99"));
        testCreateOrderCommand.setShippingAddress("123 Test Street");
        testCreateOrderCommand.setItems(List.of(itemDto));
    }
    
    @Nested
    @DisplayName("createOrder Tests")
    class CreateOrderTests {
        
        @Test
        @DisplayName("Should create order successfully")
        void shouldCreateOrderSuccessfully() {
            // Given
            PaymentInitiatedEvent paymentEvent = new PaymentInitiatedEvent();
            paymentEvent.setRazorpayOrderId("razorpay_order_123");
            paymentEvent.setOrderId("test-order-123");
            
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(paymentFeign.createPayment(any(OrderCreatedEvent.class))).thenReturn(paymentEvent);
            
            // When
            OrderCheckoutResponse response = orderService.createOrder(testCreateOrderCommand);
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getRazorpayOrderId()).isEqualTo("razorpay_order_123");
            
            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getUserId()).isEqualTo("user-456");
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("199.99"));
            
            verify(orderEventPublisher).publishInventoryLockEvent(any());
        }
    }
    
    @Nested
    @DisplayName("getUserOrders Tests")
    class GetUserOrdersTests {
        
        @Test
        @DisplayName("Should return user orders")
        void shouldReturnUserOrders() {
            // Given
            when(orderRepository.findByUserId("user-456")).thenReturn(List.of(testOrder));
            when(orderMapper.mapToDto(testOrder)).thenReturn(testOrderResponse);
            
            // When
            List<OrderResponse> orders = orderService.getUserOrders("user-456");
            
            // Then
            assertThat(orders).hasSize(1);
            assertThat(orders.get(0).getOrderNumber()).isEqualTo("test-order-123");
            
            verify(orderRepository).findByUserId("user-456");
            verify(orderMapper).mapToDto(testOrder);
        }
        
        @Test
        @DisplayName("Should return empty list when user has no orders")
        void shouldReturnEmptyListWhenNoOrders() {
            // Given
            when(orderRepository.findByUserId("user-no-orders")).thenReturn(Collections.emptyList());
            
            // When
            List<OrderResponse> orders = orderService.getUserOrders("user-no-orders");
            
            // Then
            assertThat(orders).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("getOrderDetails Tests")
    class GetOrderDetailsTests {
        
        @Test
        @DisplayName("Should return order details")
        void shouldReturnOrderDetails() {
            // Given
            when(orderRepository.findByOrderId("test-order-123")).thenReturn(Optional.of(testOrder));
            when(orderMapper.mapToDto(testOrder)).thenReturn(testOrderResponse);
            
            // When
            OrderResponse response = orderService.getOrderDetails("test-order-123");
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOrderNumber()).isEqualTo("test-order-123");
        }
        
        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void shouldThrowOrderNotFoundExceptionWhenOrderNotFound() {
            // Given
            when(orderRepository.findByOrderId("non-existent")).thenReturn(Optional.empty());
            
            // When/Then
            assertThatThrownBy(() -> orderService.getOrderDetails("non-existent"))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("non-existent");
        }
    }
    
    @Nested
    @DisplayName("updateStateOfTheOrder Tests")
    class UpdateOrderStateTests {
        
        @Test
        @DisplayName("Should update order status to SHIPPED")
        void shouldUpdateOrderStatusToShipped() {
            // Given
            testOrder.setStatus(OrderStatus.PLACED);
            when(orderRepository.findByOrderId("test-order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            
            // When
            String result = orderService.updateStateOfTheOrder("test-order-123", OrderStatus.SHIPPED);
            
            // Then
            assertThat(result).isEqualTo("Order Updated Successfully");
            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.SHIPPED);
        }
        
        @Test
        @DisplayName("Should initiate cancellation saga when status is CANCELLED")
        void shouldInitiateCancellationSagaWhenCancelled() {
            // Given
            testOrder.setStatus(OrderStatus.PLACED);
            when(orderRepository.findByOrderId("test-order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(orderMapper.mapToOrderItemDtos(any())).thenReturn(Collections.emptyList());
            
            // When
            String result = orderService.updateStateOfTheOrder("test-order-123", OrderStatus.CANCELLED);
            
            // Then
            assertThat(result).isEqualTo("Order Updated Successfully");
            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCEL_REQUESTED);
            verify(orderEventPublisher).handleOrderCancel(any());
        }
        
        @Test
        @DisplayName("Should throw exception when cancelling shipped order")
        void shouldThrowExceptionWhenCancellingShippedOrder() {
            // Given
            testOrder.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findByOrderId("test-order-123")).thenReturn(Optional.of(testOrder));
            
            // When/Then
            assertThatThrownBy(() -> orderService.updateStateOfTheOrder("test-order-123", OrderStatus.CANCELLED))
                    .isInstanceOf(OrderCancellationException.class)
                    .hasMessageContaining("cannot be cancelled after shipping");
        }
        
        @Test
        @DisplayName("Should publish ORDER_DELIVERED event when status changed to DELIVERED")
        void shouldPublishDeliveredEventWhenStatusDelivered() {
            // Given
            testOrder.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findByOrderId("test-order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            
            // When
            orderService.updateStateOfTheOrder("test-order-123", OrderStatus.DELIVERED);
            
            // Then
            verify(orderEventPublisher).publishOrderDeliveredEvent(any());
        }
        
        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Given
            when(orderRepository.findByOrderId("non-existent")).thenReturn(Optional.empty());
            
            // When/Then
            assertThatThrownBy(() -> orderService.updateStateOfTheOrder("non-existent", OrderStatus.SHIPPED))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }
    
    @Nested
    @DisplayName("updatedToPlaced Tests")
    class UpdatedToPlacedTests {
        
        @Test
        @DisplayName("Should update order to PLACED on payment success")
        void shouldUpdateOrderToPlacedOnPaymentSuccess() {
            // Given
            PaymentSuccessEvent event = new PaymentSuccessEvent();
            event.setOrderId("test-order-123");
            event.setPaymentId("pay_123456");
            
            testOrder.setStatus(OrderStatus.PAYMENT_READY);
            when(orderRepository.findByOrderId("test-order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            
            // When
            orderService.updatedToPlaced(event);
            
            // Then
            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
            assertThat(savedOrder.getPaymentId()).isEqualTo("pay_123456");
        }
        
        @Test
        @DisplayName("Should skip update if order already PLACED (idempotency)")
        void shouldSkipUpdateIfAlreadyPlaced() {
            // Given
            PaymentSuccessEvent event = new PaymentSuccessEvent();
            event.setOrderId("test-order-123");
            event.setPaymentId("pay_123456");
            
            testOrder.setStatus(OrderStatus.PLACED);
            when(orderRepository.findByOrderId("test-order-123")).thenReturn(Optional.of(testOrder));
            
            // When
            orderService.updatedToPlaced(event);
            
            // Then
            verify(orderRepository, never()).save(any());
        }
    }
    
    @Nested
    @DisplayName("cancelOrder Tests")
    class CancelOrderTests {
        
        @Test
        @DisplayName("Should cancel order due to inventory lock failure")
        void shouldCancelOrderDueToInventoryLockFailure() {
            // Given
            when(orderRepository.findByOrderId("test-order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            
            // When
            orderService.cancelOrder("test-order-123");
            
            // Then
            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderOutboxRepository).save(any());
        }
    }
}
