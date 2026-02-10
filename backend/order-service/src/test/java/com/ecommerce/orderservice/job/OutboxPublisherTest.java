package com.ecommerce.orderservice.job;

import com.ecommerce.order.OrderNotificationEvent;
import com.ecommerce.order.OrderNotificationType;
import com.ecommerce.order.OrderPayload;
import com.ecommerce.orderservice.entity.OrderOutbox;
import com.ecommerce.orderservice.entity.OutboxStatus;
import com.ecommerce.orderservice.repository.OrderOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxPublisher Unit Tests")
class OutboxPublisherTest {

    @Mock
    private OrderOutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    @Captor
    private ArgumentCaptor<OrderOutbox> outboxCaptor;

    private OrderOutbox testOutbox;
    private OrderNotificationEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new OrderNotificationEvent(
                "event-123",
                OrderNotificationType.ORDER_PLACED,
                "user-456",
                Instant.now(),
                1,
                new OrderPayload("order-789", new BigDecimal("299.99"), "USD", 2, null)
        );

        testOutbox = OrderOutbox.builder()
                .eventId("event-123")
                .aggregateId("order-789")
                .eventType("ORDER_PLACED")
                .payload("{\"eventId\":\"event-123\",\"type\":\"ORDER_PLACED\"}")
                .status(OutboxStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("publishOutboxEvents Tests")
    class PublishOutboxEventsTests {

        @Test
        @DisplayName("Should publish pending outbox events")
        void shouldPublishPendingOutboxEvents() throws Exception {
            // Given
            when(outboxRepository.findByStatus(OutboxStatus.PENDING)).thenReturn(List.of(testOutbox));
            when(objectMapper.readValue(anyString(), eq(OrderNotificationEvent.class))).thenReturn(testEvent);
            
            CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(
                    createMockSendResult()
            );
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            // When
            outboxPublisher.publishOutboxEvents();

            // Then
            verify(outboxRepository).findByStatus(OutboxStatus.PENDING);
            verify(kafkaTemplate).send(anyString(), eq("event-123"), any(OrderNotificationEvent.class));
        }

        @Test
        @DisplayName("Should update status to SENT on successful publish")
        void shouldUpdateStatusToSentOnSuccess() throws Exception {
            // Given
            when(outboxRepository.findByStatus(OutboxStatus.PENDING)).thenReturn(List.of(testOutbox));
            when(objectMapper.readValue(anyString(), eq(OrderNotificationEvent.class))).thenReturn(testEvent);
            
            CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            // When
            outboxPublisher.publishOutboxEvents();
            
            // Complete the future to trigger the callback
            future.complete(createMockSendResult());
            
            // Allow async processing
            Thread.sleep(100);

            // Then
            verify(outboxRepository).save(outboxCaptor.capture());
            assertThat(outboxCaptor.getValue().getStatus()).isEqualTo(OutboxStatus.SENT);
        }

        @Test
        @DisplayName("Should not update status on failed publish")
        void shouldNotUpdateStatusOnFailure() throws Exception {
            // Given
            when(outboxRepository.findByStatus(OutboxStatus.PENDING)).thenReturn(List.of(testOutbox));
            when(objectMapper.readValue(anyString(), eq(OrderNotificationEvent.class))).thenReturn(testEvent);
            
            CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            // When
            outboxPublisher.publishOutboxEvents();
            
            // Complete exceptionally to trigger failure callback
            future.completeExceptionally(new RuntimeException("Kafka send failed"));
            
            // Allow async processing
            Thread.sleep(100);

            // Then - Status should remain PENDING (verify no save with SENT status)
            verify(outboxRepository, never()).save(argThat(outbox -> outbox.getStatus() == OutboxStatus.SENT));
        }

        @Test
        @DisplayName("Should handle empty pending events list")
        void shouldHandleEmptyPendingList() {
            // Given
            when(outboxRepository.findByStatus(OutboxStatus.PENDING)).thenReturn(Collections.emptyList());

            // When
            outboxPublisher.publishOutboxEvents();

            // Then
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should handle JSON processing error")
        void shouldHandleJsonProcessingError() throws Exception {
            // Given
            when(outboxRepository.findByStatus(OutboxStatus.PENDING)).thenReturn(List.of(testOutbox));
            when(objectMapper.readValue(anyString(), eq(OrderNotificationEvent.class)))
                    .thenThrow(new RuntimeException("JSON parse error"));

            // When
            outboxPublisher.publishOutboxEvents();

            // Then
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should process multiple pending events")
        void shouldProcessMultiplePendingEvents() throws Exception {
            // Given
            OrderOutbox outbox2 = OrderOutbox.builder()
                    .eventId("event-456")
                    .aggregateId("order-999")
                    .eventType("ORDER_CANCELLED")
                    .payload("{\"eventId\":\"event-456\"}")
                    .status(OutboxStatus.PENDING)
                    .build();

            when(outboxRepository.findByStatus(OutboxStatus.PENDING)).thenReturn(List.of(testOutbox, outbox2));
            when(objectMapper.readValue(anyString(), eq(OrderNotificationEvent.class))).thenReturn(testEvent);
            
            CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(
                    createMockSendResult()
            );
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            // When
            outboxPublisher.publishOutboxEvents();

            // Then
            verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any());
        }
    }

    private SendResult<String, Object> createMockSendResult() {
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>("test-topic", "key", "value");
        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition("test-topic", 0), 0L, 0, 0L, 0, 0
        );
        return new SendResult<>(producerRecord, recordMetadata);
    }
}
