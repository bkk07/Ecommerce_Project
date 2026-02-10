package com.ecommerce.orderservice.mapper;

import com.ecommerce.order.OrderItemDto;
import com.ecommerce.orderservice.dto.OrderItemResponse;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderMapper Unit Tests")
class OrderMapperTest {

    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        orderMapper = new OrderMapper();
    }

    @Nested
    @DisplayName("mapToDto Tests")
    class MapToDtoTests {

        @Test
        @DisplayName("Should map Order entity to OrderResponse DTO")
        void shouldMapOrderToOrderResponse() {
            // Given
            OrderItem item = OrderItem.builder()
                    .skuCode("SKU-TEST-001")
                    .productName("Test Product")
                    .price(new BigDecimal("99.99"))
                    .quantity(2)
                    .imageUrl("http://example.com/image.jpg")
                    .build();

            Order order = Order.builder()
                    .orderId("order-123")
                    .userId("user-456")
                    .status(OrderStatus.PLACED)
                    .totalAmount(new BigDecimal("199.98"))
                    .shippingAddress("123 Test Lane")
                    .createdAt(LocalDateTime.of(2024, 1, 15, 10, 30))
                    .items(List.of(item))
                    .build();

            // When
            OrderResponse response = orderMapper.mapToDto(order);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOrderNumber()).isEqualTo("order-123");
            assertThat(response.getStatus()).isEqualTo("PLACED");
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("199.98"));
            assertThat(response.getShippingAddress()).isEqualTo("123 Test Lane");
            assertThat(response.getOrderDate()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30));
            assertThat(response.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("Should handle order with empty items list")
        void shouldHandleOrderWithEmptyItems() {
            // Given
            Order order = Order.builder()
                    .orderId("order-empty")
                    .userId("user-789")
                    .status(OrderStatus.PENDING)
                    .totalAmount(BigDecimal.ZERO)
                    .shippingAddress("Empty Address")
                    .createdAt(LocalDateTime.now())
                    .items(Collections.emptyList())
                    .build();

            // When
            OrderResponse response = orderMapper.mapToDto(order);

            // Then
            assertThat(response.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("mapToItemDtos Tests")
    class MapToItemDtosTests {

        @Test
        @DisplayName("Should map OrderItems to OrderItemResponses")
        void shouldMapOrderItemsToResponses() {
            // Given
            List<OrderItem> items = List.of(
                    OrderItem.builder()
                            .skuCode("SKU-001")
                            .productName("Product A")
                            .price(new BigDecimal("49.99"))
                            .quantity(2)
                            .imageUrl("http://example.com/a.jpg")
                            .build(),
                    OrderItem.builder()
                            .skuCode("SKU-002")
                            .productName("Product B")
                            .price(new BigDecimal("29.99"))
                            .quantity(1)
                            .imageUrl("http://example.com/b.jpg")
                            .build()
            );

            // When
            List<OrderItemResponse> responses = orderMapper.mapToItemDtos(items);

            // Then
            assertThat(responses).hasSize(2);
            
            assertThat(responses.get(0).getSkuCode()).isEqualTo("SKU-001");
            assertThat(responses.get(0).getProductName()).isEqualTo("Product A");
            assertThat(responses.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("49.99"));
            assertThat(responses.get(0).getQuantity()).isEqualTo(2);
            
            assertThat(responses.get(1).getSkuCode()).isEqualTo("SKU-002");
            assertThat(responses.get(1).getProductName()).isEqualTo("Product B");
        }

        @Test
        @DisplayName("Should return empty list for empty input")
        void shouldReturnEmptyListForEmptyInput() {
            // When
            List<OrderItemResponse> responses = orderMapper.mapToItemDtos(Collections.emptyList());

            // Then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("mapToOrderItemDtos Tests")
    class MapToOrderItemDtosTests {

        @Test
        @DisplayName("Should map OrderItems to OrderItemDtos for events")
        void shouldMapOrderItemsToOrderItemDtos() {
            // Given
            List<OrderItem> items = List.of(
                    OrderItem.builder()
                            .skuCode("SKU-EVENT-001")
                            .productName("Event Product")
                            .price(new BigDecimal("149.99"))
                            .quantity(3)
                            .imageUrl("http://example.com/event.jpg")
                            .build()
            );

            // When
            List<OrderItemDto> dtos = orderMapper.mapToOrderItemDtos(items);

            // Then
            assertThat(dtos).hasSize(1);
            
            OrderItemDto dto = dtos.get(0);
            assertThat(dto.getSkuCode()).isEqualTo("SKU-EVENT-001");
            assertThat(dto.getProductName()).isEqualTo("Event Product");
            assertThat(dto.getPrice()).isEqualByComparingTo(new BigDecimal("149.99"));
            assertThat(dto.getQuantity()).isEqualTo(3);
            assertThat(dto.getImageUrl()).isEqualTo("http://example.com/event.jpg");
        }

        @Test
        @DisplayName("Should handle multiple items")
        void shouldHandleMultipleItems() {
            // Given
            List<OrderItem> items = List.of(
                    OrderItem.builder().skuCode("SKU-1").productName("P1").price(new BigDecimal("10")).quantity(1).build(),
                    OrderItem.builder().skuCode("SKU-2").productName("P2").price(new BigDecimal("20")).quantity(2).build(),
                    OrderItem.builder().skuCode("SKU-3").productName("P3").price(new BigDecimal("30")).quantity(3).build()
            );

            // When
            List<OrderItemDto> dtos = orderMapper.mapToOrderItemDtos(items);

            // Then
            assertThat(dtos).hasSize(3);
            assertThat(dtos).extracting(OrderItemDto::getSkuCode)
                    .containsExactly("SKU-1", "SKU-2", "SKU-3");
        }
    }
}
