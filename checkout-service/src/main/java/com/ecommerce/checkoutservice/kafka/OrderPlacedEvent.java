package com.ecommerce.checkoutservice.kafka;
import com.ecommerce.checkout.CheckoutItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPlacedEvent {
    private String orderId;
    private Long userId;
    private List<CheckoutItem> items;
    private BigDecimal amount;
}