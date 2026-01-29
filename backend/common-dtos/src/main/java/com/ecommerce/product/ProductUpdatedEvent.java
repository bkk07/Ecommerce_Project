package com.ecommerce.product;
public class ProductUpdatedEvent{
    private String productId;
    private String name;
    private boolean isActive;

    public ProductUpdatedEvent(String productId, String name, boolean isActive) {
        this.productId = productId;
        this.name = name;
        this.isActive = isActive;
    }
    public ProductUpdatedEvent() {

    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
