package com.ecommerce.checkout;

import com.ecommerce.order.AddressDTO;
import com.ecommerce.order.OrderItemDto;
import java.math.BigDecimal;
import java.util.List;

public class CreateOrderCommand {
    private String userId;
    private BigDecimal totalAmount;
    private String  shippingAddress;
    private List<OrderItemDto> items;

    public CreateOrderCommand(String userId, BigDecimal totalAmount, String shippingAddress, List<OrderItemDto> items) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.items = items;
    }
    public CreateOrderCommand() {}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
}
