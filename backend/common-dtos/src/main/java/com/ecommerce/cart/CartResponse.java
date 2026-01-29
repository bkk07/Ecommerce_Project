package com.ecommerce.cart;
import java.math.BigDecimal;
import java.util.List;

public class CartResponse {
    private String userId;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
    public CartResponse(){}
    public CartResponse(String userId, List<CartItemResponse> items, BigDecimal totalAmount) {
        this.userId = userId;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}