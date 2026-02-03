package com.ecommerce.ratingservice.kafka;

import com.ecommerce.order.OrderDeliveredEvent;
import com.ecommerce.ratingservice.entity.RatingEligibility;
import com.ecommerce.ratingservice.repository.RatingEligibilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Kafka consumers.
 */
@ExtendWith(MockitoExtension.class)
class OrderDeliveredConsumerTest {

    @Mock
    private RatingEligibilityRepository eligibilityRepository;

    @InjectMocks
    private OrderDeliveredConsumer orderDeliveredConsumer;

    private static final String ORDER_ID = "order-123";
    private static final String USER_ID = "user-456";

    @BeforeEach
    void setUp() {
        // No additional setup needed
    }

    @Test
    @DisplayName("Should create eligibility records for delivered order items")
    void shouldCreateEligibilityForDeliveredOrder() {
        // Given
        OrderDeliveredEvent.DeliveredItem item1 = new OrderDeliveredEvent.DeliveredItem(
                "PROD-001", "Test Product 1", "http://example.com/img1.jpg");
        OrderDeliveredEvent.DeliveredItem item2 = new OrderDeliveredEvent.DeliveredItem(
                "PROD-002", "Test Product 2", "http://example.com/img2.jpg");
        
        OrderDeliveredEvent event = new OrderDeliveredEvent(
                ORDER_ID, USER_ID, List.of(item1, item2));

        when(eligibilityRepository.findByOrderIdAndSku(any(), any()))
                .thenReturn(Optional.empty());

        // When
        orderDeliveredConsumer.handleOrderDelivered(event);

        // Then
        ArgumentCaptor<RatingEligibility> captor = ArgumentCaptor.forClass(RatingEligibility.class);
        verify(eligibilityRepository, times(2)).save(captor.capture());

        List<RatingEligibility> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(e -> e.getOrderId().equals(ORDER_ID));
        assertThat(saved).allMatch(e -> e.getUserId().equals(USER_ID));
        assertThat(saved).allMatch(e -> Boolean.TRUE.equals(e.getCanRate()));
    }

    @Test
    @DisplayName("Should handle order without items gracefully")
    void shouldHandleOrderWithoutItems() {
        // Given
        OrderDeliveredEvent event = new OrderDeliveredEvent(
                ORDER_ID, USER_ID, Collections.emptyList());

        // When
        orderDeliveredConsumer.handleOrderDelivered(event);

        // Then
        verify(eligibilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip existing eligibility records (idempotency)")
    void shouldSkipExistingEligibility() {
        // Given
        OrderDeliveredEvent.DeliveredItem item = new OrderDeliveredEvent.DeliveredItem(
                "PROD-001", "Test Product", "http://example.com/img.jpg");
        
        OrderDeliveredEvent event = new OrderDeliveredEvent(
                ORDER_ID, USER_ID, List.of(item));

        RatingEligibility existingEligibility = RatingEligibility.builder()
                .orderId(ORDER_ID)
                .sku("PROD-001")
                .build();
        
        when(eligibilityRepository.findByOrderIdAndSku(ORDER_ID, "PROD-001"))
                .thenReturn(Optional.of(existingEligibility));

        // When
        orderDeliveredConsumer.handleOrderDelivered(event);

        // Then
        verify(eligibilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle null items gracefully")
    void shouldHandleNullItems() {
        // Given
        OrderDeliveredEvent event = new OrderDeliveredEvent(ORDER_ID, USER_ID, null);

        // When/Then - should not throw
        orderDeliveredConsumer.handleOrderDelivered(event);
        verify(eligibilityRepository, never()).save(any());
    }
}
