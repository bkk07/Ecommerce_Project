package com.ecommerce.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for admin order list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderPageResponse {
    private List<AdminOrderResponse> orders;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}
