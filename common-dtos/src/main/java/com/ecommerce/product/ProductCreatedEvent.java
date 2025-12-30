package com.ecommerce.product;

import java.math.BigDecimal;
public class ProductCreatedEvent{
    private Long productId;
    private String name;
    private String sku;
    private String description;
    private BigDecimal price;
    public ProductCreatedEvent(Long productId, String name, String sku, String description, BigDecimal price) {
        this.productId = productId;
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.price = price;
    }

    public ProductCreatedEvent(){}
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public BigDecimal getPrice() {
        return price;
    }
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
