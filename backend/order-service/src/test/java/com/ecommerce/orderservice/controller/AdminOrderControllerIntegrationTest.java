package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.kafka.OrderEventPublisher;
import com.ecommerce.orderservice.repository.OrderRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminOrderController Integration Tests")
class AdminOrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        
        // Create test orders with different statuses
        createTestOrder("admin-order-1", "user-1", OrderStatus.PENDING, new BigDecimal("100.00"), 
                LocalDateTime.now().minusDays(1));
        createTestOrder("admin-order-2", "user-2", OrderStatus.PLACED, new BigDecimal("200.00"), 
                LocalDateTime.now().minusDays(2));
        createTestOrder("admin-order-3", "user-3", OrderStatus.SHIPPED, new BigDecimal("300.00"), 
                LocalDateTime.now().minusDays(3));
        createTestOrder("admin-order-4", "user-1", OrderStatus.DELIVERED, new BigDecimal("400.00"), 
                LocalDateTime.now().minusDays(10));
        createTestOrder("admin-order-5", "user-2", OrderStatus.CANCELLED, new BigDecimal("500.00"), 
                LocalDateTime.now().minusDays(5));
    }

    private void createTestOrder(String orderId, String userId, OrderStatus status, BigDecimal amount, LocalDateTime createdAt) {
        Order order = Order.builder()
                .orderId(orderId)
                .userId(userId)
                .status(status)
                .totalAmount(amount)
                .shippingAddress("Admin Test Address")
                .createdAt(createdAt)
                .build();

        OrderItem item = OrderItem.builder()
                .skuCode("ADMIN-SKU-" + orderId)
                .productName("Admin Product " + orderId)
                .price(amount)
                .quantity(1)
                .imageUrl("http://example.com/" + orderId + ".jpg")
                .order(order)
                .build();

        order.setItems(List.of(item));
        orderRepository.save(order);
    }

    @Nested
    @DisplayName("GET /admin/orders/stats")
    class GetOrderStatsTests {

        @Test
        @DisplayName("Should return order statistics")
        void shouldReturnOrderStatistics() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/admin/orders/stats")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalOrders").value(5))
                    .andExpect(jsonPath("$.ordersByStatus").exists())
                    .andExpect(jsonPath("$.ordersByStatus.pending").value(1))
                    .andExpect(jsonPath("$.ordersByStatus.placed").value(1))
                    .andExpect(jsonPath("$.ordersByStatus.shipped").value(1))
                    .andExpect(jsonPath("$.ordersByStatus.delivered").value(1))
                    .andExpect(jsonPath("$.ordersByStatus.cancelled").value(1))
                    .andExpect(jsonPath("$.revenueStats").exists())
                    .andExpect(jsonPath("$.generatedAt").exists());
        }
    }

    @Nested
    @DisplayName("GET /admin/orders")
    class GetAllOrdersTests {

        @Test
        @DisplayName("Should return paginated orders")
        void shouldReturnPaginatedOrders() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/admin/orders")
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(5)))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.currentPage").value(0));
        }

        @Test
        @DisplayName("Should return orders with pagination")
        void shouldReturnOrdersWithPagination() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/admin/orders")
                    .param("page", "0")
                    .param("size", "2")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("Should return orders sorted by date descending")
        void shouldReturnOrdersSortedByDateDesc() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/admin/orders")
                    .param("page", "0")
                    .param("size", "10")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", "desc")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].orderId").value("admin-order-1")); // Most recent
        }
    }

    @Nested
    @DisplayName("GET /admin/orders/status/{status}")
    class GetOrdersByStatusTests {

        @Test
        @DisplayName("Should return orders filtered by PLACED status")
        void shouldReturnOrdersByPlacedStatus() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/admin/orders/status/PLACED")
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].status").value("PLACED"));
        }

        @Test
        @DisplayName("Should return orders filtered by DELIVERED status")
        void shouldReturnOrdersByDeliveredStatus() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/admin/orders/status/DELIVERED")
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].status").value("DELIVERED"));
        }

        @Test
        @DisplayName("Should return empty list for status with no orders")
        void shouldReturnEmptyForStatusWithNoOrders() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/admin/orders/status/PACKED")
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /admin/orders/date-range")
    class GetOrdersByDateRangeTests {

        @Test
        @DisplayName("Should return orders within date range")
        void shouldReturnOrdersWithinDateRange() throws Exception {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(4);
            LocalDateTime endDate = LocalDateTime.now();

            // When
            ResultActions result = mockMvc.perform(get("/admin/orders/date-range")
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString())
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
        }
    }

    @Nested
    @DisplayName("GET /admin/orders/search")
    class SearchOrdersTests {

        @Test
        @DisplayName("Should search orders by user ID")
        void shouldSearchOrdersByUserId() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/admin/orders/search")
                    .param("keyword", "user-1")
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2))) // user-1 has 2 orders
                    .andExpect(jsonPath("$.content[*].userId", everyItem(is("user-1"))));
        }

        @Test
        @DisplayName("Should search orders by order ID")
        void shouldSearchOrdersByOrderId() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/admin/orders/search")
                    .param("keyword", "admin-order-3")
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].orderId").value("admin-order-3"));
        }

        @Test
        @DisplayName("Should return empty for non-matching search")
        void shouldReturnEmptyForNonMatchingSearch() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/admin/orders/search")
                    .param("keyword", "nonexistent")
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /admin/orders/{orderId}")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return order by ID")
        void shouldReturnOrderById() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/admin/orders/admin-order-2")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value("admin-order-2"))
                    .andExpect(jsonPath("$.userId").value("user-2"))
                    .andExpect(jsonPath("$.status").value("PLACED"))
                    .andExpect(jsonPath("$.totalAmount").value(200.00))
                    .andExpect(jsonPath("$.items", hasSize(1)));
        }

        @Test
        @DisplayName("Should return 404 for non-existent order")
        void shouldReturn404ForNonExistentOrder() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/admin/orders/non-existent-order")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }
    }
}
