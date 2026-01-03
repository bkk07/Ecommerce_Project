package com.ecommerce.orderservice.service;

import com.ecommerce.inventory.InventoryReleasedEvent;
import com.ecommerce.order.OrderCancelEvent;
import com.ecommerce.order.OrderItemDto;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.SagaState;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.kafka.OrderEventPublisher;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.repository.SagaStateRepository;
import com.ecommerce.payment.PaymentRefundedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorService {
    private final SagaStateRepository sagaStateRepository;
    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderMapper orderMapper;
    @Transactional
    public void handleInventoryReleased(InventoryReleasedEvent event) {
        log.info("Saga: Inventory Released for Order: {}", event.getOrderId());
        SagaState state = getOrCreateSagaState(event.getOrderId());
        state.setInventoryReleased(true);
        sagaStateRepository.save(state);
        checkAndFinalizeCancellation(event.getOrderId());
    }
    @Transactional
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        log.info("Saga: Payment Refunded for Order: {}", event.getOrderId());
        SagaState state = getOrCreateSagaState(event.getOrderId());
        state.setPaymentRefunded(true);
        sagaStateRepository.save(state);
        checkAndFinalizeCancellation(event.getOrderId());
    }

    private SagaState getOrCreateSagaState(String orderId) {
        return sagaStateRepository.findById(orderId)
                .orElseGet(() -> SagaState.builder()
                        .orderId(orderId)
                        .inventoryReleased(false)
                        .paymentRefunded(false)
                        .build());
    }

    private void checkAndFinalizeCancellation(String orderId) {
        SagaState state = sagaStateRepository.findById(orderId).orElseThrow();
        
        Order order = orderRepository.findByOrderId(orderId).orElse(null);
        if (order != null && order.getStatus() == OrderStatus.CANCEL_REQUESTED) {
             // We wait for both inventory release and payment refund confirmation
             if (state.isInventoryReleased() && state.isPaymentRefunded()) {
                 order.setStatus(OrderStatus.CANCELLED);
                 orderRepository.save(order);
                 log.info("Saga: Order {} marked as CANCELLED", orderId);
             }
        }
    }

    /**
     * Scheduled Job: Runs every 1 minute.
     * Finds Sagas that have been stuck for more than 5 minutes.
     */
    @Scheduled(fixedRate = 60000) // Run every 1 minute
    @Transactional
    public void retryStuckSagas() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        List<SagaState> stuckSagas = sagaStateRepository.findStuckSagas(cutoff);

        if (!stuckSagas.isEmpty()) {
            log.info("Found {} stuck sagas. Retrying...", stuckSagas.size());
        }

        for (SagaState saga : stuckSagas) {
            // Re-trigger the cancellation event
            // This assumes downstream services are idempotent (which they should be)
            Order order = orderRepository.findByOrderId(saga.getOrderId()).orElse(null);
            if (order != null && order.getStatus() == OrderStatus.CANCEL_REQUESTED) {
                log.info("Retrying cancellation for Order: {}", saga.getOrderId());
                
                OrderCancelEvent orderCancelEvent = new OrderCancelEvent();
                orderCancelEvent.setOrderId(order.getOrderId());
                orderCancelEvent.setUserId(order.getUserId());
                List<OrderItemDto> orderItemDtos = orderMapper.mapToOrderItemDtos(order.getItems());
                orderCancelEvent.setItems(orderItemDtos);
                orderEventPublisher.handleOrderCancel(orderCancelEvent);
                
                // Update timestamp to avoid immediate re-pick in next run
                saga.setUpdatedAt(LocalDateTime.now());
                sagaStateRepository.save(saga);
            }
        }
    }
}
