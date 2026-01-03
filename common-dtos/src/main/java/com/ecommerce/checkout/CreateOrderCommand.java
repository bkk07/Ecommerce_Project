package com.ecommerce.checkout;

import com.ecommerce.order.AddressDTO;
import com.ecommerce.order.OrderItemDto;
import java.math.BigDecimal;
import java.util.List;

public class CreateOrderCommand {
    private String userId;
    private BigDecimal totalAmount;
    private AddressDTO addressDTO;
    private List<OrderItemDto> items;

    public CreateOrderCommand() {
    }

    public CreateOrderCommand(String userId, BigDecimal totalAmount, AddressDTO addressDTO, List<OrderItemDto> items) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.addressDTO = addressDTO;
        this.items = items;
    }

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

    public AddressDTO getAddressDTO() {
        return addressDTO;
    }

    public void setAddressDTO(AddressDTO addressDTO) {
        this.addressDTO = addressDTO;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
}
