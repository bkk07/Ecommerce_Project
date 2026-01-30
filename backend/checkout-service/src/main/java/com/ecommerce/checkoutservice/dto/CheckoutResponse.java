package com.ecommerce.checkoutservice.dto;
import com.ecommerce.product.ItemValidationResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class CheckoutResponse {

    private CheckoutStatus status;

    // success fields
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;

    // failure fields
    private CheckoutFailureReason failureReason;
    private List<ItemValidationResult> itemErrors;
}
