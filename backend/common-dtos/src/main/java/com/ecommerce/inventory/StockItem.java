package com.ecommerce.inventory;

public class StockItem {
    private String sku;
    private Integer quantity;
    public StockItem(){};
    public StockItem(String sku, Integer quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }
    public String getSku() {
        return sku;
    }
    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
