package com.ecommerce.checkoutservice.service;
import com.ecommerce.cart.CartItemResponse;
import com.ecommerce.cart.CartResponse;
import com.ecommerce.checkout.CheckoutItem;
import com.ecommerce.checkout.CreateOrderCommand;
import com.ecommerce.checkoutservice.dto.*;
import com.ecommerce.checkoutservice.kafka.KafkaEventPublisher;
import com.ecommerce.checkoutservice.openfeign.CartClient;
import com.ecommerce.order.OrderItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final CartClient cartClient;
    private final KafkaEventPublisher eventPublisher;

    public CheckoutResponse initiateCheckout(InitiateCheckoutRequest request) {

        List<CheckoutItem> itemsToBuy;

        // 1. Determine Source (Cart or Direct)
        if (request.isCartId()) {
            CartResponse cartResponse = cartClient.getCart();
            itemsToBuy = mapToCartResponse(cartResponse);
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

            try {
            // 3. Publish CreateOrderCommand (Async)
            CreateOrderCommand command = new CreateOrderCommand();
            command.setUserId(String.valueOf(request.getUserId()));
            command.setTotalAmount(totalAmount);
            command.setItems(mapToOrderItemDto(itemsToBuy));
            // Add address if available in request, for now null or default
            command.setAddressDTO(null);

            eventPublisher.publishCreateOrderCommand(command);

            return new CheckoutResponse(null, totalAmount, "INR", "PROCESSING");
        } catch (Exception e) {
            throw new RuntimeException("Checkout Init Failed", e);
        }
    }

    private List<CheckoutItem> mapToCartResponse(CartResponse cartResponse) {
        List<CheckoutItem> checkoutItems= new ArrayList<>();
        for(CartItemResponse cartItemResponse:cartResponse.getItems()){
            CheckoutItem checkoutItem = new CheckoutItem();
            checkoutItem.setSkuCode(cartItemResponse.getSkuCode());
            checkoutItem.setPrice(cartItemResponse.getPrice());
            checkoutItem.setQuantity(cartItemResponse.getQuantity());
            checkoutItem.setProductName(cartItemResponse.getProductName());
            checkoutItems.add(checkoutItem);
        }
        return checkoutItems;
    }

    private List<OrderItemDto> mapToOrderItemDto(List<CheckoutItem> items){
        List<OrderItemDto> orderItemDtos = new ArrayList<>();
        for(CheckoutItem item:items){
            OrderItemDto orderItemDto = new OrderItemDto();
            orderItemDto.setSkuCode(item.getSkuCode());
            orderItemDto.setQuantity(item.getQuantity());
            orderItemDto.setProductName(item.getProductName());
            orderItemDto.setPrice(item.getPrice());
            orderItemDtos.add(orderItemDto);
        }
        return  orderItemDtos;
    }
}
