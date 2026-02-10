package com.ecommerce.inventory;

import java.util.List;

/**
 * Request DTO for batch stock lock operations.
 * Used for synchronous inventory lock requests.
 */
public class BatchStockLockRequest {
    private List<StockItem> items;
    private String orderId;

    public BatchStockLockRequest() {
    }

    public BatchStockLockRequest(List<StockItem> items, String orderId) {
        this.items = items;
        this.orderId = orderId;
    }

    public List<StockItem> getItems() {
        return items;
    }

    public void setItems(List<StockItem> items) {
        this.items = items;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
