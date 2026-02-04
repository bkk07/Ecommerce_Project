package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventory.*;
import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.model.Inventory;
import com.ecommerce.inventoryservice.model.InventoryEventType;
import com.ecommerce.inventoryservice.model.OutboxEvent;
import com.ecommerce.inventoryservice.model.StockReservation;
import com.ecommerce.inventoryservice.producer.InventoryEventProducer;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ecommerce.inventoryservice.repository.OutboxEventRepository;
import com.ecommerce.inventoryservice.repository.StockReservationRepository;
import com.ecommerce.order.OrderItemDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ecommerce.common.KafkaProperties.*;

/**
 * Service for managing product inventory.
 * Handles stock initialization, updates, reservations, and releases.
 * Uses the Transactional Outbox Pattern for reliable event publishing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryEventProducer inventoryEventProducer;
    private final StockReservationRepository stockReservationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    /**
     * Get inventory by SKU code.
     * 
     * @param skuCode The SKU code to look up
     * @return Optional containing the inventory response if found
     */
    @Transactional(readOnly = true)
    @RateLimiter(name = "inventoryRead")
    public Optional<InventoryResponse> getInventoryBySkuCode(String skuCode) {
        log.debug("Looking up inventory for SKU: {}", skuCode);
        return inventoryRepository.findBySkuCode(skuCode)
                .map(this::mapToResponse);
    }

    /**
     * Get all inventory items.
     * 
     * @return List of all inventory responses
     */
    @Transactional(readOnly = true)
    @RateLimiter(name = "inventoryRead")
    public List<InventoryResponse> getAllInventory() {
        log.debug("Fetching all inventory items");
        return inventoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get inventory for multiple SKU codes.
     * 
     * @param skuCodes List of SKU codes to look up
     * @return List of inventory responses for found SKUs
     */
    @Transactional(readOnly = true)
    @RateLimiter(name = "inventoryRead")
    public List<InventoryResponse> getInventoryBySkuCodes(List<String> skuCodes) {
        log.debug("Looking up inventory for {} SKUs", skuCodes.size());
        return inventoryRepository.findBySkuCodeIn(skuCodes).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Check if a SKU is in stock.
     * 
     * @param skuCode The SKU code to check
     * @return true if available stock > 0
     */
    @Transactional(readOnly = true)
    public boolean isInStock(String skuCode) {
        return inventoryRepository.findBySkuCode(skuCode)
                .map(inv -> inv.getAvailableStock() > 0)
                .orElse(false);
    }

    /**
     * Maps Inventory entity to InventoryResponse DTO.
     */
    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .skuCode(inventory.getSkuCode())
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableStock(inventory.getAvailableStock())
                .inStock(inventory.getAvailableStock() > 0)
                .build();
    }

    @Transactional
    @CircuitBreaker(name = "default")
    public void initStock(String skuCode) {
        if (inventoryRepository.findBySkuCode(skuCode).isPresent()) {
            return;
        }
        Inventory inventory = new Inventory();
        inventory.setSkuCode(skuCode);
        inventory.setQuantity(0);
        inventory.setReservedQuantity(0);
        inventoryRepository.save(inventory);
    }
    @Transactional
    public void updateStock(String skuCode, Integer quantity) {
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new InventoryNotFoundException(skuCode));

        inventory.setQuantity(quantity);
        inventoryRepository.save(inventory);

        InventoryUpdatedEvent event = new InventoryUpdatedEvent();
        event.setSkuCode(skuCode);
        event.setNewQuantity(quantity);
        event.setAvailable(quantity > 0);

        saveToOutbox(skuCode, InventoryEventType.INVENTORY_UPDATED_EVENT, event, INVENTORY_EVENTS_TOPIC);
    }

    @Transactional
    public void reserveStock(String skuCode, Integer quantity, String orderId) {
        // 1. Idempotency Check: If order already reserved this SKU, return success immediately
        if (stockReservationRepository.findByOrderIdAndSkuCode(orderId, skuCode).isPresent()) {
            log.info("Stock already reserved for Order: {} SKU: {}. Skipping duplicate request.", orderId, skuCode);
            return;
        }
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new InventoryNotFoundException(skuCode));
        log.info("Reserving stock for SKU: {} with quantity: {}", skuCode, quantity);
        if (inventory.getAvailableStock() < quantity) {
            throw new InsufficientStockException("Not enough stock for SKU: " + skuCode);
        }
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);

        // Track Reservation for Idempotency
        StockReservation reservation = StockReservation.builder()
                .orderId(orderId)
                .skuCode(skuCode)
                .quantity(quantity)
                .status(StockReservation.ReservationStatus.RESERVED)
                .build();
        stockReservationRepository.save(reservation);

        InventoryUpdatedEvent event = new InventoryUpdatedEvent();
        event.setSkuCode(skuCode);
        event.setNewQuantity(inventory.getQuantity());
        event.setAvailable(inventory.getAvailableStock() > 0);

        saveToOutbox(skuCode, InventoryEventType.INVENTORY_UPDATED_EVENT, event, INVENTORY_EVENTS_TOPIC);
    }

    @Transactional
    public void reserveStock(List<StockItem> items, String orderId) {
        for (StockItem item : items) {
            reserveStock(item.getSku(), item.getQuantity(), orderId);
        }
    }

    @Transactional
    public void releaseStock(String skuCode, Integer quantity, String orderId) {
        // 1. Idempotency Check using StockReservation
        StockReservation reservation = stockReservationRepository.findByOrderIdAndSkuCode(orderId, skuCode)
                .orElse(null);

        if (reservation == null) {
            log.warn("No reservation found for Order: {} SKU: {}. Ignoring release request.", orderId, skuCode);
            // We still emit event to let Saga complete, assuming it was never reserved or already handled
            InventoryReleasedEvent releasedEvent = new InventoryReleasedEvent(orderId, skuCode, 0);
            saveToOutbox(orderId, InventoryEventType.INVENTORY_RELEASED_EVENT, releasedEvent, INVENTORY_RELEASED_EVENTS_TOPIC);
            return;
        }

        if (reservation.getStatus() == StockReservation.ReservationStatus.RELEASED) {
            log.info("Stock already released for Order: {} SKU: {}. Skipping.", orderId, skuCode);
            // Even if already released, we should probably ensure the event is sent if it was missed,
            // but for now we assume if it's marked released, the event was sent.
            // However, to be safe for Saga completion, we can re-emit or just return.
            // Let's re-emit to be safe in case the previous event was lost before being sent to Kafka.
             InventoryReleasedEvent releasedEvent = new InventoryReleasedEvent(orderId, skuCode, reservation.getQuantity());
             saveToOutbox(orderId, InventoryEventType.INVENTORY_RELEASED_EVENT, releasedEvent, INVENTORY_RELEASED_EVENTS_TOPIC);
            return;
        }

        // 2. Business Logic
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new InventoryNotFoundException(skuCode));

        log.info("Releasing stock for SKU: {} with quantity: {} for Order: {}", skuCode, quantity, orderId);

        // Use the quantity from the reservation to be safe
        int quantityToRelease = reservation.getQuantity();

        if (inventory.getReservedQuantity() >= quantityToRelease) {
            inventory.setReservedQuantity(inventory.getReservedQuantity() - quantityToRelease);
            inventoryRepository.save(inventory);
        } else {
             log.warn("Reserved quantity {} is less than release quantity {} for SKU {}. Adjusting to release all reserved.", inventory.getReservedQuantity(), quantityToRelease, skuCode);
             inventory.setReservedQuantity(0);
             inventoryRepository.save(inventory);
        }
        
        // 3. Update Reservation Status
        reservation.setStatus(StockReservation.ReservationStatus.RELEASED);
        stockReservationRepository.save(reservation);

        // 4. Outbox Pattern
        InventoryReleasedEvent releasedEvent = new InventoryReleasedEvent(orderId, skuCode, quantityToRelease);
        saveToOutbox(orderId, InventoryEventType.INVENTORY_RELEASED_EVENT, releasedEvent, INVENTORY_RELEASED_EVENTS_TOPIC);
    }

    @Transactional
    public void releaseStock(List<StockItem> items, String orderId) {
        for (StockItem item : items) {
            releaseStock(item.getSku(), item.getQuantity(), orderId);
        }
    }

    @Transactional
    public void lockStockForOrder(InventoryLockEvent event) {
        try {
            for (OrderItemDto item : event.getItems()) {
                reserveStock(item.getSkuCode(), item.getQuantity(), event.getOrderId());
            }
        } catch (InsufficientStockException | InventoryNotFoundException e) {
            log.warn("Failed to lock stock for order: {}. Reason: {}", event.getOrderId(), e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            applicationContext.getBean(InventoryService.class).handleLockFailure(event.getOrderId(), e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleLockFailure(String orderId, String reason) {
        InventoryLockFailedEvent failedEvent = new InventoryLockFailedEvent(orderId, reason);
        saveToOutbox(orderId, InventoryEventType.INVENTORY_LOCK_FAILED_EVENT, failedEvent, INVENTORY_LOCK_FAILED_TOPIC);
    }

    private void saveToOutbox(String aggregateId, InventoryEventType eventType, Object event, String topic) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .eventType(eventType.getEventName())
                    .payload(payload)
                    .topic(topic)
                    .processed(false)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing event for outbox", e);
        }
    }
}
