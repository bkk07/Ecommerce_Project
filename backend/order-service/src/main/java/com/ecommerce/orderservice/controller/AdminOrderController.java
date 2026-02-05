package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.AdminOrderPageResponse;
import com.ecommerce.orderservice.dto.AdminOrderResponse;
import com.ecommerce.orderservice.dto.ErrorResponse;
import com.ecommerce.orderservice.dto.OrderStatsResponse;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Admin Orders", description = "Admin order management - statistics, search, and order details")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @Operation(
            summary = "Get order statistics",
            description = """
                    Get comprehensive order statistics for the admin dashboard including:
                    - Total orders count
                    - Orders by status breakdown
                    - Revenue statistics (today, week, month, year)
                    - New orders statistics
                    - Fulfillment statistics
                    
                    Results are cached for 5 minutes.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderStatsResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/stats")
    public ResponseEntity<OrderStatsResponse> getOrderStats() {
        log.info("Admin request: fetching order statistics");
        OrderStatsResponse stats = adminOrderService.getOrderStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Get all orders (paginated)",
            description = "Retrieve a paginated list of all orders with sorting options."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminOrderPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<AdminOrderPageResponse> getAllOrders(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        log.info("Admin request: fetching all orders - page={}, size={}, sortBy={}, sortDirection={}", 
                page, size, sortBy, sortDirection);
        
        AdminOrderPageResponse response = adminOrderService.getAllOrders(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get orders by status",
            description = "Retrieve orders filtered by a specific status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminOrderPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status or pagination parameters",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<AdminOrderPageResponse> getOrdersByStatus(
            @Parameter(description = "Order status to filter by", required = true, example = "PLACED")
            @PathVariable OrderStatus status,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        
        log.info("Admin request: fetching orders by status={} - page={}, size={}", status, page, size);
        
        AdminOrderPageResponse response = adminOrderService.getOrdersByStatus(status, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get orders by date range",
            description = "Retrieve orders created within a specific date range."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminOrderPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range or pagination parameters",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/date-range")
    public ResponseEntity<AdminOrderPageResponse> getOrdersByDateRange(
            @Parameter(description = "Start date (ISO format)", required = true, example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true, example = "2024-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        
        log.info("Admin request: fetching orders from {} to {} - page={}, size={}", 
                startDate, endDate, page, size);
        
        AdminOrderPageResponse response = adminOrderService.getOrdersByDateRange(startDate, endDate, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Search orders",
            description = "Search orders by order ID or user ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminOrderPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<AdminOrderPageResponse> searchOrders(
            @Parameter(description = "Search keyword (order ID or user ID)", required = true, example = "user-123")
            @RequestParam @NotBlank(message = "Search keyword is required") String keyword,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        
        log.info("Admin request: searching orders with keyword='{}' - page={}, size={}", keyword, page, size);
        
        AdminOrderPageResponse response = adminOrderService.searchOrders(keyword, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get order by ID",
            description = "Retrieve detailed order information including all items, payment details, and timestamps."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminOrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderResponse> getOrderById(
            @Parameter(description = "Order ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable @NotBlank(message = "Order ID is required") String orderId) {
        log.info("Admin request: fetching order details for orderId={}", orderId);
        
        AdminOrderResponse order = adminOrderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }
}
