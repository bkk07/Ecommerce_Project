package com.ecommerce.orderservice.controller;

import com.ecommerce.checkout.CreateOrderCommand;
import com.ecommerce.order.OrderItemDto;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.feign.PaymentFeign;
import com.ecommerce.orderservice.kafka.OrderEventPublisher;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.payment.PaymentInitiatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("OrderController Integration Tests")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private PaymentFeign paymentFeign;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        
        testOrder = Order.builder()
                .orderId("integration-order-123")
                .userId("integration-user-456")
                .status(OrderStatus.PLACED)
                .totalAmount(new BigDecimal("399.99"))
                .shippingAddress("789 Integration Blvd")
                .createdAt(LocalDateTime.now())
                .build();

        OrderItem item = OrderItem.builder()
                .skuCode("INT-SKU-001")
                .productName("Integration Test Product")
                .price(new BigDecimal("399.99"))
                .quantity(1)
                .imageUrl("http://example.com/int.jpg")
                .order(testOrder)
                .build();

        testOrder.setItems(List.of(item));
        orderRepository.save(testOrder);
    }

    @Nested
    @DisplayName("GET /api/orders/user/{userId}")
    class GetUserOrdersTests {

        @Test
        @DisplayName("Should return orders for user")
        void shouldReturnOrdersForUser() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/orders/user/integration-user-456")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].orderNumber").value("integration-order-123"))
                    .andExpect(jsonPath("$[0].status").value("PLACED"))
                    .andExpect(jsonPath("$[0].totalAmount").value(399.99));
        }

        @Test
        @DisplayName("Should return empty list for user with no orders")
        void shouldReturnEmptyListForNoOrders() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/orders/user/non-existent-user")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/orders/{orderNumber}")
    class GetOrderDetailsTests {

        @Test
        @DisplayName("Should return order details")
        void shouldReturnOrderDetails() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/orders/integration-order-123")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNumber").value("integration-order-123"))
                    .andExpect(jsonPath("$.status").value("PLACED"))
                    .andExpect(jsonPath("$.totalAmount").value(399.99))
                    .andExpect(jsonPath("$.shippingAddress").value("789 Integration Blvd"))
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].skuCode").value("INT-SKU-001"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent order")
        void shouldReturn404ForNonExistentOrder() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/orders/non-existent-order")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value(containsString("non-existent-order")));
        }
    }

    @Nested
    @DisplayName("PUT /api/orders/{orderNumber}")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Should update order status to SHIPPED")
        void shouldUpdateOrderStatusToShipped() throws Exception {
            // When
            ResultActions result = mockMvc.perform(put("/api/orders/integration-order-123")
                    .param("status", "SHIPPED")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(content().string("Order Updated Successfully"));

            // Verify in database
            Order updatedOrder = orderRepository.findByOrderId("integration-order-123").orElseThrow();
            assert updatedOrder.getStatus() == OrderStatus.SHIPPED;
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent order")
        void shouldReturn404WhenUpdatingNonExistentOrder() throws Exception {
            // When
            ResultActions result = mockMvc.perform(put("/api/orders/non-existent-order")
                    .param("status", "SHIPPED")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 for invalid status")
        void shouldReturn400ForInvalidStatus() throws Exception {
            // When
            ResultActions result = mockMvc.perform(put("/api/orders/integration-order-123")
                    .param("status", "INVALID_STATUS")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/orders/create/order")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully")
        void shouldCreateOrderSuccessfully() throws Exception {
            // Given
            OrderItemDto itemDto = new OrderItemDto();
            itemDto.setSkuCode("NEW-SKU-001");
            itemDto.setProductName("New Product");
            itemDto.setPrice(new BigDecimal("149.99"));
            itemDto.setQuantity(2);
            itemDto.setImageUrl("http://example.com/new.jpg");

            CreateOrderCommand command = new CreateOrderCommand();
            command.setUserId("new-user-789");
            command.setTotalAmount(new BigDecimal("299.98"));
            command.setShippingAddress("123 New Street");
            command.setItems(List.of(itemDto));

            PaymentInitiatedEvent paymentEvent = new PaymentInitiatedEvent();
            paymentEvent.setRazorpayOrderId("razorpay_new_123");
            paymentEvent.setOrderId("new-order-id");

            when(paymentFeign.createPayment(any())).thenReturn(paymentEvent);

            // When
            ResultActions result = mockMvc.perform(post("/api/orders/create/order")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.razorpayOrderId").value("razorpay_new_123"));
        }
    }
}
