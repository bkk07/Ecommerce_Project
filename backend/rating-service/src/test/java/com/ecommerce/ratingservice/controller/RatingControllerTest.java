package com.ecommerce.ratingservice.controller;

import com.ecommerce.ratingservice.dto.*;
import com.ecommerce.ratingservice.enums.RatingStatus;
import com.ecommerce.ratingservice.exception.GlobalExceptionHandler;
import com.ecommerce.ratingservice.exception.ResourceNotFoundException;
import com.ecommerce.ratingservice.service.RatingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for RatingController.
 */
@ExtendWith(MockitoExtension.class)
class RatingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RatingService ratingService;

    @InjectMocks
    private RatingController ratingController;

    private ObjectMapper objectMapper;

    private static final String USER_ID = "user-123";
    private static final String SKU = "PROD-001";
    private static final String ORDER_ID = "order-456";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(ratingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("Create Rating Tests")
    class CreateRatingTests {

        @Test
        @DisplayName("Should create rating successfully")
        void shouldCreateRating() throws Exception {
            // Given
            CreateRatingRequest request = CreateRatingRequest.builder()
                    .sku(SKU)
                    .orderId(ORDER_ID)
                    .rating(5)
                    .message("Great product!")
                    .build();

            RatingResponse response = createRatingResponse(1L);

            when(ratingService.createRating(any(CreateRatingRequest.class), eq(USER_ID)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/ratings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Auth-User-Id", USER_ID)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.sku").value(SKU))
                    .andExpect(jsonPath("$.rating").value(5));
        }

        @Test
        @DisplayName("Should return 400 for invalid rating value")
        void shouldReturn400ForInvalidRating() throws Exception {
            // Given
            CreateRatingRequest request = CreateRatingRequest.builder()
                    .sku(SKU)
                    .orderId(ORDER_ID)
                    .rating(6) // Invalid - max is 5
                    .build();

            // When/Then
            mockMvc.perform(post("/api/v1/ratings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Auth-User-Id", USER_ID)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        void shouldReturn400ForMissingFields() throws Exception {
            // Given
            CreateRatingRequest request = CreateRatingRequest.builder()
                    .rating(5)
                    // Missing sku and orderId
                    .build();

            // When/Then
            mockMvc.perform(post("/api/v1/ratings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Auth-User-Id", USER_ID)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get Product Ratings Tests")
    class GetProductRatingsTests {

        @Test
        @DisplayName("Should get product ratings with pagination")
        void shouldGetProductRatings() throws Exception {
            // Given
            PagedResponse<RatingResponse> pagedResponse = PagedResponse.<RatingResponse>builder()
                    .content(List.of(createRatingResponse(1L), createRatingResponse(2L)))
                    .page(0)
                    .size(20)
                    .totalElements(2)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(ratingService.getProductRatingsPaged(eq(SKU), eq(0), anyInt()))
                    .thenReturn(pagedResponse);

            // When/Then
            mockMvc.perform(get("/api/v1/ratings/product/{sku}", SKU)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("Should get product rating summary")
        void shouldGetProductRatingSummary() throws Exception {
            // Given
            ProductRatingSummary summary = ProductRatingSummary.builder()
                    .sku(SKU)
                    .averageRating(4.5)
                    .totalRatings(100L)
                    .fiveStarCount(50L)
                    .fourStarCount(30L)
                    .threeStarCount(15L)
                    .twoStarCount(3L)
                    .oneStarCount(2L)
                    .build();

            when(ratingService.getProductRatingSummary(SKU))
                    .thenReturn(summary);

            // When/Then
            mockMvc.perform(get("/api/v1/ratings/product/{sku}/summary", SKU))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sku").value(SKU))
                    .andExpect(jsonPath("$.averageRating").value(4.5))
                    .andExpect(jsonPath("$.totalRatings").value(100));
        }
    }

    @Nested
    @DisplayName("Get Rating By ID Tests")
    class GetRatingByIdTests {

        @Test
        @DisplayName("Should get rating by ID")
        void shouldGetRatingById() throws Exception {
            // Given
            RatingResponse response = createRatingResponse(1L);

            when(ratingService.getRatingById(1L))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/ratings/{ratingId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.sku").value(SKU));
        }

        @Test
        @DisplayName("Should return 404 for non-existent rating")
        void shouldReturn404ForNonExistentRating() throws Exception {
            // Given
            when(ratingService.getRatingById(999L))
                    .thenThrow(new ResourceNotFoundException("Rating", "999"));

            // When/Then
            mockMvc.perform(get("/api/v1/ratings/{ratingId}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Update Rating Tests")
    class UpdateRatingTests {

        @Test
        @DisplayName("Should update rating successfully")
        void shouldUpdateRating() throws Exception {
            // Given
            UpdateRatingRequest request = UpdateRatingRequest.builder()
                    .rating(4)
                    .message("Updated review")
                    .build();

            RatingResponse response = createRatingResponse(1L);
            response.setRating(4);

            when(ratingService.updateRating(eq(1L), any(UpdateRatingRequest.class), eq(USER_ID)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(put("/api/v1/ratings/{ratingId}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Auth-User-Id", USER_ID)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rating").value(4));
        }
    }

    @Nested
    @DisplayName("Delete Rating Tests")
    class DeleteRatingTests {

        @Test
        @DisplayName("Should delete rating successfully")
        void shouldDeleteRating() throws Exception {
            // Given
            doNothing().when(ratingService).deleteRating(1L, USER_ID);

            // When/Then
            mockMvc.perform(delete("/api/v1/ratings/{ratingId}", 1L)
                            .header("X-Auth-User-Id", USER_ID))
                    .andExpect(status().isNoContent());

            verify(ratingService).deleteRating(1L, USER_ID);
        }
    }

    // ==================== HELPER METHODS ====================

    private RatingResponse createRatingResponse(Long id) {
        RatingResponse response = new RatingResponse();
        response.setId(id);
        response.setSku(SKU);
        response.setOrderId(ORDER_ID);
        response.setUserId(USER_ID);
        response.setRating(5);
        response.setMessage("Great product!");
        response.setIsVerifiedPurchase(true);
        response.setStatus(RatingStatus.APPROVED);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }
}
