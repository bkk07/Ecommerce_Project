package com.ecommerce.product;

import java.math.BigDecimal;
import java.util.List;

public class ProductCreatedEvent{
    private Long productId;
    private String name;
    private String sku;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private List<String> categories;
    public ProductCreatedEvent(){};
    public ProductCreatedEvent(Long productId, String name, String sku, String description, BigDecimal price, String imageUrl, List<String> categories) {
        this.productId = productId;
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.categories = categories;
    }

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}
