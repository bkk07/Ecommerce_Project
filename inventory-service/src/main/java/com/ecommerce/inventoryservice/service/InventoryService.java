package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventory.*;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.model.Inventory;
import com.ecommerce.inventoryservice.model.OutboxEvent;
import com.ecommerce.inventoryservice.model.StockReservation;
import com.ecommerce.inventoryservice.producer.InventoryEventProducer;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ecommerce.inventoryservice.repository.OutboxEventRepository;
import com.ecommerce.inventoryservice.repository.StockReservationRepository;
import com.ecommerce.order.OrderItemDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ecommerce.common.KafkaProperties.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryEventProducer inventoryEventProducer;
    private final StockReservationRepository stockReservationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
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

        saveToOutbox(skuCode, "InventoryUpdatedEvent", event, INVENTORY_EVENTS_TOPIC);
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

        saveToOutbox(skuCode, "InventoryUpdatedEvent", event, INVENTORY_EVENTS_TOPIC);
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
            saveToOutbox(orderId, "InventoryReleasedEvent", releasedEvent, INVENTORY_RELEASED_EVENTS_TOPIC);
            return;
        }

        if (reservation.getStatus() == StockReservation.ReservationStatus.RELEASED) {
            log.info("Stock already released for Order: {} SKU: {}. Skipping.", orderId, skuCode);
            // Even if already released, we should probably ensure the event is sent if it was missed,
            // but for now we assume if it's marked released, the event was sent.
            // However, to be safe for Saga completion, we can re-emit or just return.
            // Let's re-emit to be safe in case the previous event was lost before being sent to Kafka.
             InventoryReleasedEvent releasedEvent = new InventoryReleasedEvent(orderId, skuCode, reservation.getQuantity());
             saveToOutbox(orderId, "InventoryReleasedEvent", releasedEvent, INVENTORY_RELEASED_EVENTS_TOPIC);
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
        saveToOutbox(orderId, "InventoryReleasedEvent", releasedEvent, INVENTORY_RELEASED_EVENTS_TOPIC);
    }

    @Transactional
    public void releaseStock(List<StockItem> items, String orderId) {
        for (StockItem item : items) {
            releaseStock(item.getSku(), item.getQuantity(), orderId);
        }
    }

    @Transactional
    public void lockStockForOrder(InventoryLockEvent event) {
        for (OrderItemDto item : event.getItems()) {
            reserveStock(item.getSkuCode(), item.getQuantity(), event.getOrderId());
        }
    }

    private void saveToOutbox(String aggregateId, String eventType, Object event, String topic) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .eventType(eventType)
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
