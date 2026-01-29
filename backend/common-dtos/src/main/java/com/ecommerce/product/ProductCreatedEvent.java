package com.ecommerce.product;

import java.math.BigDecimal;
public class ProductCreatedEvent{
    private Long productId;
    private String name;
    private String sku;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String category;
    public ProductCreatedEvent(Long productId, String name, String sku, String description, BigDecimal price,String imageUrl,String category) {
        this.productId = productId;
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category=category;
    }

    public ProductCreatedEvent(){}
    public String getCategory(){
        return  category;
    }
    public void  setCategory(String category){
        this.category=category;
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
}
