package com.ecommerce.checkoutservice.service;

import com.ecommerce.cart.CartResponse;
import com.ecommerce.checkout.CheckoutItem;
import com.ecommerce.checkout.CreateOrderCommand;
import com.ecommerce.checkout.OrderCheckoutResponse;
import com.ecommerce.checkoutservice.config.CheckoutConfig;
import com.ecommerce.checkoutservice.dto.CheckoutFailureReason;
import com.ecommerce.checkoutservice.dto.CheckoutResponse;
import com.ecommerce.checkoutservice.dto.CheckoutStatus;
import com.ecommerce.checkoutservice.dto.InitiateCheckoutRequest;
import com.ecommerce.checkoutservice.entity.CheckoutSession;
import com.ecommerce.checkoutservice.mapper.CheckoutMapper;
import com.ecommerce.checkoutservice.openfeign.CartClient;
import com.ecommerce.checkoutservice.openfeign.OrderClient;
import com.ecommerce.checkoutservice.openfeign.ProductClient;
import com.ecommerce.checkoutservice.repository.CheckoutSessionRepository;
import com.ecommerce.product.ProductValidationItem;
import com.ecommerce.product.ProductValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final CartClient cartClient;
    private final OrderClient orderClient;
    private final ProductClient productClient;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final CheckoutMapper checkoutMapper;
    public CheckoutResponse initiateCheckout(InitiateCheckoutRequest request) {

        List<CheckoutItem> itemsToBuy;

        // 1. Determine Source (Cart or Direct)
        if (request.isCartId()) {
            CartResponse cartResponse = cartClient.getCart();
            itemsToBuy = checkoutMapper.toCheckoutItems(cartResponse.getItems());
            log.info("The Items to Buy are from cart :{}", itemsToBuy);
        } else {
            itemsToBuy = request.getItems();
            log.info("The Items to Buy are from request :{}", itemsToBuy);
        }

        if (itemsToBuy == null || itemsToBuy.isEmpty()) {
            log.info("The Items to Buy are from request empty");
            throw new IllegalArgumentException("No items to checkout");

        }

        // 2. Validate Products (SYNC)
        List<ProductValidationItem> validationItems =
                checkoutMapper.toProductValidationItems(itemsToBuy);

        ProductValidationResponse validationResponse =
                productClient.validateProducts(validationItems);
        log.info("Completed validation");
        // 🔴 🔴 🔴 THIS IS WHAT YOU NEED TO IMPLEMENT 🔴 🔴 🔴
        if (validationResponse == null || !validationResponse.isValid()) {
            log.warn("Checkout failed due to product validation errors: {}",
                    validationResponse != null ? validationResponse.getResults() : "No response from product service");

            return new CheckoutResponse(
                    CheckoutStatus.FAILED,
                    null,
                    null,
                    CheckoutConfig.CURRENCY,
                    CheckoutFailureReason.PRODUCT_VALIDATION_FAILED,
                    validationResponse != null ? validationResponse.getResults() : null
            );
        }
        // 3. Calculate Total (ONLY IF VALID)
        BigDecimal totalAmount = itemsToBuy.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("Total Amount{}", totalAmount);
        // 4. Create Order Command
        CreateOrderCommand command = new CreateOrderCommand();
        command.setUserId(String.valueOf(request.getUserId()));
        command.setTotalAmount(totalAmount);
        command.setItems(checkoutMapper.toOrderItemDtos(itemsToBuy));
        command.setShippingAddress(request.getShippingAddress());
        log.info("Before order-response");
        // 5. Call Order Service
        OrderCheckoutResponse orderResponse =
                orderClient.createCheckoutOrder(command);
        log.info("after order-response");
        String razorpayOrderId = orderResponse.getRazorpayOrderId();
        log.info("Razor Pay Id{}", razorpayOrderId);
        // 6. Save Session
        CheckoutSession session = CheckoutSession.builder()
                .orderId(razorpayOrderId)
                .userId(request.getUserId())
                .cartId(request.isCartId())
                .totalAmount(totalAmount)
                .items(itemsToBuy)
                .status(CheckoutConfig.STATUS_PROCESSING)
                .build();

        checkoutSessionRepository.save(session);

        // 7. Success response
        return new CheckoutResponse(
                CheckoutStatus.SUCCESS,
                razorpayOrderId,
                totalAmount,
                CheckoutConfig.CURRENCY,
                null,
                null
        );
    }
}
