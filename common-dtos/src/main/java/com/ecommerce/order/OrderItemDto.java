package com.ecommerce.order;

import java.math.BigDecimal;
public class OrderItemDto {
    private String skuCode;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    public OrderItemDto(){}

    public OrderItemDto(String skuCode, String productName, BigDecimal price, Integer quantity) {
        this.skuCode = skuCode;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
