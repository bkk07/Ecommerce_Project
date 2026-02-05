package com.ecommerce.orderservice.controller;

import com.ecommerce.checkout.CreateOrderCommand;
import com.ecommerce.checkout.OrderCheckoutResponse;
import com.ecommerce.orderservice.dto.ErrorResponse;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Orders", description = "Customer order operations - create, view, and manage orders")
public class OrderController {
    
    private final OrderService orderService;
    
    @Operation(
            summary = "Get user orders",
            description = "Retrieve all orders for a specific user. Returns a list of orders sorted by creation date (newest first)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid user ID",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @Parameter(description = "User ID", required = true, example = "user-123")
            @PathVariable @NotBlank(message = "User ID is required") String userId) {
        log.info("Fetching orders for user: {}", userId);
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @Operation(
            summary = "Get order details",
            description = "Retrieve detailed information about a specific order including all items and payment status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order details retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderDetails(
            @Parameter(description = "Order number/ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable @NotBlank(message = "Order number is required") String orderNumber) {
        log.info("Fetching order details for: {}", orderNumber);
        return ResponseEntity.ok(orderService.getOrderDetails(orderNumber));
    }

    @Operation(
            summary = "Update order status",
            description = """
                    Update the status of an order. Available status transitions:
                    - PENDING → PAYMENT_READY → PLACED → PACKED → SHIPPED → DELIVERED
                    - Any status → CANCELLED (triggers refund saga if payment was made)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status transition",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{orderNumber}")
    public ResponseEntity<String> updateOrderStatus(
            @Parameter(description = "Order number/ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable @NotBlank(message = "Order number is required") String orderNumber,
            @Parameter(description = "New order status", required = true, example = "SHIPPED")
            @RequestParam OrderStatus status) {
        log.info("Updating order {} status to {}", orderNumber, status);
        return ResponseEntity.ok(orderService.updateStateOfTheOrder(orderNumber, status));
    }
    
    @Operation(
            summary = "Create checkout order",
            description = """
                    Create a new order from checkout and initiate payment process.
                    This endpoint:
                    1. Creates the order in PENDING status
                    2. Locks inventory for the items
                    3. Creates Razorpay payment order
                    4. Returns the Razorpay order ID for frontend payment
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order created successfully, payment initiated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderCheckoutResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid order data",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "402", description = "Payment creation failed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Payment service unavailable",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/create/order")
    public ResponseEntity<OrderCheckoutResponse> createCheckoutOrder(
            @Parameter(description = "Order creation command with items and shipping details")
            @Valid @RequestBody CreateOrderCommand createOrderCommand) {
        log.info("Creating order for user: {}", createOrderCommand.getUserId());
        return ResponseEntity.ok(orderService.createOrder(createOrderCommand));
    }
}