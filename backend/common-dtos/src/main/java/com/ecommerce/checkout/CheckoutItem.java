package com.ecommerce.checkout;

import java.math.BigDecimal;

public class CheckoutItem {
    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal price;
    private String skuCode;
    public CheckoutItem(){};

    public CheckoutItem(Long productId, String productName, int quantity, BigDecimal price, String skuCode) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.skuCode = skuCode;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }
}