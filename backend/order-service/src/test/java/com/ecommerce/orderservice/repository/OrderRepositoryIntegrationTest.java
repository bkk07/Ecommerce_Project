package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("OrderRepository Integration Tests")
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        
        // Create test orders
        createOrder("repo-order-1", "repo-user-1", OrderStatus.PENDING, new BigDecimal("100.00"), 
                LocalDateTime.now().minusDays(1));
        createOrder("repo-order-2", "repo-user-1", OrderStatus.PLACED, new BigDecimal("200.00"), 
                LocalDateTime.now().minusDays(2));
        createOrder("repo-order-3", "repo-user-2", OrderStatus.SHIPPED, new BigDecimal("300.00"), 
                LocalDateTime.now().minusDays(3));
        createOrder("repo-order-4", "repo-user-3", OrderStatus.DELIVERED, new BigDecimal("400.00"), 
                LocalDateTime.now().minusDays(10));
        createOrder("repo-order-5", "repo-user-2", OrderStatus.CANCELLED, new BigDecimal("500.00"), 
                LocalDateTime.now().minusDays(5));
    }

    private void createOrder(String orderId, String userId, OrderStatus status, BigDecimal amount, LocalDateTime createdAt) {
        Order order = Order.builder()
                .orderId(orderId)
                .userId(userId)
                .status(status)
                .totalAmount(amount)
                .shippingAddress("Test Address")
                .createdAt(createdAt)
                .build();

        OrderItem item = OrderItem.builder()
                .skuCode("SKU-" + orderId)
                .productName("Product " + orderId)
                .price(amount)
                .quantity(1)
                .order(order)
                .build();

        order.setItems(List.of(item));
        orderRepository.save(order);
    }

    @Nested
    @DisplayName("findByOrderId Tests")
    class FindByOrderIdTests {

        @Test
        @DisplayName("Should find order by ID")
        void shouldFindOrderById() {
            // When
            Optional<Order> result = orderRepository.findByOrderId("repo-order-1");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getOrderId()).isEqualTo("repo-order-1");
            assertThat(result.get().getUserId()).isEqualTo("repo-user-1");
        }

        @Test
        @DisplayName("Should return empty for non-existent order")
        void shouldReturnEmptyForNonExistent() {
            // When
            Optional<Order> result = orderRepository.findByOrderId("non-existent");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserId Tests")
    class FindByUserIdTests {

        @Test
        @DisplayName("Should find all orders for user")
        void shouldFindAllOrdersForUser() {
            // When
            List<Order> orders = orderRepository.findByUserId("repo-user-1");

            // Then
            assertThat(orders).hasSize(2);
            assertThat(orders).extracting(Order::getOrderId)
                    .containsExactlyInAnyOrder("repo-order-1", "repo-order-2");
        }

        @Test
        @DisplayName("Should return empty list for user with no orders")
        void shouldReturnEmptyForUserWithNoOrders() {
            // When
            List<Order> orders = orderRepository.findByUserId("non-existent-user");

            // Then
            assertThat(orders).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByStatus Tests")
    class CountByStatusTests {

        @Test
        @DisplayName("Should count orders by status")
        void shouldCountOrdersByStatus() {
            // When & Then
            assertThat(orderRepository.countByStatus(OrderStatus.PENDING)).isEqualTo(1);
            assertThat(orderRepository.countByStatus(OrderStatus.PLACED)).isEqualTo(1);
            assertThat(orderRepository.countByStatus(OrderStatus.SHIPPED)).isEqualTo(1);
            assertThat(orderRepository.countByStatus(OrderStatus.DELIVERED)).isEqualTo(1);
            assertThat(orderRepository.countByStatus(OrderStatus.CANCELLED)).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return zero for status with no orders")
        void shouldReturnZeroForStatusWithNoOrders() {
            // When
            long count = orderRepository.countByStatus(OrderStatus.PACKED);

            // Then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("findByStatus Tests")
    class FindByStatusTests {

        @Test
        @DisplayName("Should find orders by status with pagination")
        void shouldFindOrdersByStatusWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Order> page = orderRepository.findByStatus(OrderStatus.PLACED, pageable);

            // Then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getStatus()).isEqualTo(OrderStatus.PLACED);
        }
    }

    @Nested
    @DisplayName("findByCreatedAtBetween Tests")
    class FindByCreatedAtBetweenTests {

        @Test
        @DisplayName("Should find orders within date range")
        void shouldFindOrdersWithinDateRange() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(4);
            LocalDateTime endDate = LocalDateTime.now();
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Order> page = orderRepository.findByCreatedAtBetween(startDate, endDate, pageable);

            // Then
            assertThat(page.getContent()).isNotEmpty();
            page.getContent().forEach(order -> {
                assertThat(order.getCreatedAt()).isAfterOrEqualTo(startDate);
                assertThat(order.getCreatedAt()).isBeforeOrEqualTo(endDate);
            });
        }
    }

    @Nested
    @DisplayName("searchOrders Tests")
    class SearchOrdersTests {

        @Test
        @DisplayName("Should search orders by user ID")
        void shouldSearchOrdersByUserId() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Order> page = orderRepository.searchOrders("repo-user-2", pageable);

            // Then
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent()).extracting(Order::getUserId)
                    .containsOnly("repo-user-2");
        }

        @Test
        @DisplayName("Should search orders by order ID")
        void shouldSearchOrdersByOrderId() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Order> page = orderRepository.searchOrders("repo-order-3", pageable);

            // Then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getOrderId()).isEqualTo("repo-order-3");
        }
    }

    @Nested
    @DisplayName("sumRevenueByStatuses Tests")
    class SumRevenueByStatusesTests {

        @Test
        @DisplayName("Should sum revenue for specified statuses")
        void shouldSumRevenueForStatuses() {
            // Given
            List<OrderStatus> revenueStatuses = List.of(OrderStatus.PLACED, OrderStatus.DELIVERED);

            // When
            BigDecimal revenue = orderRepository.sumRevenueByStatuses(revenueStatuses);

            // Then - PLACED (200) + DELIVERED (400) = 600
            assertThat(revenue).isEqualByComparingTo(new BigDecimal("600.00"));
        }

        @Test
        @DisplayName("Should return zero when no orders match statuses")
        void shouldReturnZeroWhenNoOrdersMatch() {
            // Given
            List<OrderStatus> emptyStatuses = List.of(OrderStatus.PACKED);

            // When
            BigDecimal revenue = orderRepository.sumRevenueByStatuses(emptyStatuses);

            // Then
            assertThat(revenue).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("countOrdersSince Tests")
    class CountOrdersSinceTests {

        @Test
        @DisplayName("Should count orders since date")
        void shouldCountOrdersSinceDate() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusDays(4);

            // When
            long count = orderRepository.countOrdersSince(since);

            // Then
            assertThat(count).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("existsByPaymentId Tests")
    class ExistsByPaymentIdTests {

        @Test
        @DisplayName("Should check if payment ID exists")
        void shouldCheckIfPaymentIdExists() {
            // Given
            Order order = orderRepository.findByOrderId("repo-order-1").orElseThrow();
            order.setPaymentId("pay_test_123");
            orderRepository.save(order);

            // When & Then
            assertThat(orderRepository.existsByPaymentId("pay_test_123")).isTrue();
            assertThat(orderRepository.existsByPaymentId("non_existent_pay")).isFalse();
        }
    }
}
