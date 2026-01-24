package com.ecommerce.inventoryservice.model;

public enum InventoryEventType {
    INVENTORY_UPDATED_EVENT("InventoryUpdatedEvent"),
    INVENTORY_RELEASED_EVENT("InventoryReleasedEvent"),
    INVENTORY_LOCK_FAILED_EVENT("InventoryLockFailedEvent");

    private final String eventName;

    InventoryEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}
