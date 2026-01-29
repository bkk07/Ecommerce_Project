package com.ecommerce.checkoutservice.service;

import com.ecommerce.cart.CartResponse;
import com.ecommerce.checkout.CheckoutItem;
import com.ecommerce.checkout.CreateOrderCommand;
import com.ecommerce.checkout.OrderCheckoutResponse;
import com.ecommerce.checkoutservice.config.CheckoutConfig;
import com.ecommerce.checkoutservice.dto.CheckoutResponse;
import com.ecommerce.checkoutservice.dto.InitiateCheckoutRequest;
import com.ecommerce.checkoutservice.entity.CheckoutSession;
import com.ecommerce.checkoutservice.mapper.CheckoutMapper;
import com.ecommerce.checkoutservice.openfeign.CartClient;
import com.ecommerce.checkoutservice.openfeign.OrderClient;
import com.ecommerce.checkoutservice.repository.CheckoutSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final CartClient cartClient;
    private final OrderClient orderClient;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final CheckoutMapper checkoutMapper;

    public CheckoutResponse initiateCheckout(InitiateCheckoutRequest request) {
        List<CheckoutItem> itemsToBuy;
        // 1. Determine Source (Cart or Direct)
        if (request.isCartId()) {
            CartResponse cartResponse = cartClient.getCart();
            itemsToBuy = checkoutMapper.toCheckoutItems(cartResponse.getItems());
        } else {
            itemsToBuy = request.getItems();
        }
        if (itemsToBuy == null || itemsToBuy.isEmpty()) {
            throw new IllegalArgumentException("No items to checkout");
        }
        // 2. Calculate Total
        BigDecimal totalAmount = itemsToBuy.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Create Order Command
        CreateOrderCommand command = new CreateOrderCommand();
        command.setUserId(String.valueOf(request.getUserId()));
        command.setTotalAmount(totalAmount);
        command.setItems(checkoutMapper.toOrderItemDtos(itemsToBuy));
        command.setAddressDTO(null);

        // 4. Call Order Service to get Razorpay Order ID
        OrderCheckoutResponse orderResponse = orderClient.createCheckoutOrder(command);
        String razorpayOrderId = orderResponse.getRazorpayOrderId();

        // 5. Save Session to Redis
        CheckoutSession session = CheckoutSession.builder()
                .orderId(razorpayOrderId)
                .userId(request.getUserId())
                .cartId(request.isCartId())
                .totalAmount(totalAmount)
                .items(itemsToBuy)
                .status(CheckoutConfig.STATUS_PROCESSING)
                .build();
        
        checkoutSessionRepository.save(session);

        return new CheckoutResponse(razorpayOrderId, totalAmount, CheckoutConfig.CURRENCY, CheckoutConfig.STATUS_PROCESSING);
    }
}
