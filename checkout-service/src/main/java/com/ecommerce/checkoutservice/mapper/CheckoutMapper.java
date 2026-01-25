package com.ecommerce.checkoutservice.mapper;

import com.ecommerce.cart.CartItemResponse;
import com.ecommerce.checkout.CheckoutItem;
import com.ecommerce.order.OrderItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CheckoutMapper {

    CheckoutItem toCheckoutItem(CartItemResponse cartItemResponse);

    List<CheckoutItem> toCheckoutItems(List<CartItemResponse> cartItemResponses);

    OrderItemDto toOrderItemDto(CheckoutItem checkoutItem);

    List<OrderItemDto> toOrderItemDtos(List<CheckoutItem> checkoutItems);
}
