package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.AdminOrderPageResponse;
import com.ecommerce.orderservice.dto.AdminOrderResponse;
import com.ecommerce.orderservice.dto.OrderStatsResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrderService Unit Tests")
class AdminOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AdminOrderService adminOrderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .orderId("test-order-123")
                .userId("user-456")
                .status(OrderStatus.PLACED)
                .totalAmount(new BigDecimal("299.99"))
                .shippingAddress("456 Admin Street")
                .createdAt(LocalDateTime.now())
                .items(List.of(
                        OrderItem.builder()
                                .skuCode("SKU-002")
                                .productName("Admin Test Product")
                                .price(new BigDecimal("299.99"))
                                .quantity(1)
                                .build()
                ))
                .build();
    }

    @Nested
    @DisplayName("getOrderStats Tests")
    class GetOrderStatsTests {

        @Test
        @DisplayName("Should return order statistics")
        void shouldReturnOrderStatistics() {
            // Given
            when(orderRepository.count()).thenReturn(100L);
            when(orderRepository.countByStatus(any(OrderStatus.class))).thenReturn(10L);
            when(orderRepository.sumRevenueByStatuses(anyList())).thenReturn(new BigDecimal("50000.00"));
            when(orderRepository.sumRevenueSince(anyList(), any(LocalDateTime.class))).thenReturn(new BigDecimal("1000.00"));
            when(orderRepository.countOrdersSince(any(LocalDateTime.class))).thenReturn(5L);

            // When
            OrderStatsResponse stats = adminOrderService.getOrderStats();

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.getTotalOrders()).isEqualTo(100L);
            assertThat(stats.getOrdersByStatus()).isNotNull();
            assertThat(stats.getRevenueStats()).isNotNull();
            assertThat(stats.getGeneratedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getAllOrders Tests")
    class GetAllOrdersTests {

        @Test
        @DisplayName("Should return paginated orders")
        void shouldReturnPaginatedOrders() {
            // Given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
            when(orderRepository.findAll(any(Pageable.class))).thenReturn(orderPage);

            // When
            AdminOrderPageResponse response = adminOrderService.getAllOrders(0, 10, "createdAt", "desc");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
            
            verify(orderRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page when no orders")
        void shouldReturnEmptyPageWhenNoOrders() {
            // Given
            Page<Order> emptyPage = new PageImpl<>(List.of());
            when(orderRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            // When
            AdminOrderPageResponse response = adminOrderService.getAllOrders(0, 10, "createdAt", "desc");

            // Then
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Should sort orders ascending")
        void shouldSortOrdersAscending() {
            // Given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
            when(orderRepository.findAll(any(Pageable.class))).thenReturn(orderPage);

            // When
            AdminOrderPageResponse response = adminOrderService.getAllOrders(0, 10, "createdAt", "asc");

            // Then
            assertThat(response).isNotNull();
            verify(orderRepository).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getOrdersByStatus Tests")
    class GetOrdersByStatusTests {

        @Test
        @DisplayName("Should return orders filtered by status")
        void shouldReturnOrdersFilteredByStatus() {
            // Given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
            when(orderRepository.findByStatus(any(OrderStatus.class), any(Pageable.class))).thenReturn(orderPage);

            // When
            AdminOrderPageResponse response = adminOrderService.getOrdersByStatus(OrderStatus.PLACED, 0, 10);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            
            verify(orderRepository).findByStatus(OrderStatus.PLACED, any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getOrdersByDateRange Tests")
    class GetOrdersByDateRangeTests {

        @Test
        @DisplayName("Should return orders within date range")
        void shouldReturnOrdersWithinDateRange() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(7);
            LocalDateTime endDate = LocalDateTime.now();
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
            when(orderRepository.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(orderPage);

            // When
            AdminOrderPageResponse response = adminOrderService.getOrdersByDateRange(startDate, endDate, 0, 10);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("searchOrders Tests")
    class SearchOrdersTests {

        @Test
        @DisplayName("Should return orders matching search keyword")
        void shouldReturnOrdersMatchingKeyword() {
            // Given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
            when(orderRepository.searchOrders(any(String.class), any(Pageable.class))).thenReturn(orderPage);

            // When
            AdminOrderPageResponse response = adminOrderService.searchOrders("user-456", 0, 10);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            
            verify(orderRepository).searchOrders("user-456", any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getOrderById Tests")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return order by ID")
        void shouldReturnOrderById() {
            // Given
            when(orderRepository.findByOrderId("test-order-123")).thenReturn(Optional.of(testOrder));

            // When
            AdminOrderResponse response = adminOrderService.getOrderById("test-order-123");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo("test-order-123");
            assertThat(response.getUserId()).isEqualTo("user-456");
            assertThat(response.getStatus()).isEqualTo("PLACED");
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void shouldThrowOrderNotFoundExceptionWhenNotFound() {
            // Given
            when(orderRepository.findByOrderId("non-existent")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adminOrderService.getOrderById("non-existent"))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("non-existent");
        }
    }
}
