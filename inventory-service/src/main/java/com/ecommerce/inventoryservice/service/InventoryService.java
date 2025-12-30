package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventory.InventoryUpdatedEvent;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.model.Inventory;
import com.ecommerce.inventoryservice.producer.InventoryEventProducer;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryEventProducer inventoryEventProducer;

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
        inventoryEventProducer.sendInventoryUpdate(event);
    }
    @Transactional
    public void reserveStock(String skuCode, Integer quantity) {
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new InventoryNotFoundException(skuCode));

        if (inventory.getAvailableStock() < quantity) {
            throw new InsufficientStockException("Not enough stock for SKU: " + skuCode);
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);
        InventoryUpdatedEvent event = new InventoryUpdatedEvent();
        event.setSkuCode(skuCode);
        event.setNewQuantity(inventory.getQuantity());
        event.setAvailable(inventory.getAvailableStock() > 0);
        inventoryEventProducer.sendInventoryUpdate(event);
    }
}
