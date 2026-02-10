package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.dto.InventoryRequest;
import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.model.Inventory;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(controllers = InventoryController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for controller tests
@ActiveProfiles("test")
@DisplayName("InventoryController Integration Tests")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        testInventory = new Inventory();
        testInventory.setId(1L);
        testInventory.setSkuCode("SKU-001");
        testInventory.setQuantity(100);
        testInventory.setReservedQuantity(10);
    }

    @Nested
    @DisplayName("GET /api/inventory/{skuCode}")
    class GetInventoryBySkuCodeTests {

        @Test
        @DisplayName("Should return inventory when SKU exists")
        void shouldReturnInventoryWhenSkuExists() throws Exception {
            // Given
            when(inventoryService.getInventoryBySkuCode("SKU-001"))
                    .thenReturn(Optional.of(testInventory));

            // When
            ResultActions result = mockMvc.perform(get("/api/inventory/{skuCode}", "SKU-001")
                    .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.skuCode").value("SKU-001"))
                    .andExpect(jsonPath("$.quantity").value(100))
                    .andExpect(jsonPath("$.reservedQuantity").value(10));
        }

        @Test
        @DisplayName("Should return 404 when SKU not found")
        void shouldReturn404WhenSkuNotFound() throws Exception {
            // Given
            when(inventoryService.getInventoryBySkuCode("INVALID"))
                    .thenReturn(Optional.empty());

            // When/Then
            mockMvc.perform(get("/api/inventory/{skuCode}", "INVALID")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/inventory")
    class GetAllInventoryTests {

        @Test
        @DisplayName("Should return all inventory items")
        void shouldReturnAllInventoryItems() throws Exception {
            // Given
            Inventory inventory2 = new Inventory();
            inventory2.setId(2L);
            inventory2.setSkuCode("SKU-002");
            inventory2.setQuantity(50);
            inventory2.setReservedQuantity(5);

            when(inventoryService.getAllInventory())
                    .thenReturn(List.of(testInventory, inventory2));

            // When/Then
            mockMvc.perform(get("/api/inventory")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].skuCode").value("SKU-001"))
                    .andExpect(jsonPath("$[1].skuCode").value("SKU-002"));
        }
    }

    @Nested
    @DisplayName("POST /api/inventory/check")
    class CheckStockTests {

        @Test
        @DisplayName("Should return in-stock status when stock available")
        void shouldReturnInStockWhenAvailable() throws Exception {
            // Given
            when(inventoryService.getInventoryBySkuCode("SKU-001"))
                    .thenReturn(Optional.of(testInventory));

            // When/Then
            mockMvc.perform(post("/api/inventory/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[{\"skuCode\":\"SKU-001\",\"quantity\":50}]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].skuCode").value("SKU-001"))
                    .andExpect(jsonPath("$[0].inStock").value(true));
        }

        @Test
        @DisplayName("Should return out-of-stock when insufficient stock")
        void shouldReturnOutOfStockWhenInsufficient() throws Exception {
            // Given
            when(inventoryService.getInventoryBySkuCode("SKU-001"))
                    .thenReturn(Optional.of(testInventory));

            // When/Then - Available = 90, requesting 100
            mockMvc.perform(post("/api/inventory/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[{\"skuCode\":\"SKU-001\",\"quantity\":100}]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].skuCode").value("SKU-001"))
                    .andExpect(jsonPath("$[0].inStock").value(false));
        }
    }

    @Nested
    @DisplayName("PUT /api/inventory/{skuCode}")
    class UpdateStockTests {

        @Test
        @DisplayName("Should update stock quantity successfully")
        void shouldUpdateStockQuantitySuccessfully() throws Exception {
            // Given
            InventoryRequest request = new InventoryRequest("SKU-001", 200);
            doNothing().when(inventoryService).updateStock("SKU-001", 200);

            // When/Then
            mockMvc.perform(put("/api/inventory/{skuCode}", "SKU-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(inventoryService).updateStock("SKU-001", 200);
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent SKU")
        void shouldReturn404WhenUpdatingNonExistentSku() throws Exception {
            // Given
            InventoryRequest request = new InventoryRequest("INVALID", 200);
            doThrow(new InventoryNotFoundException("INVALID"))
                    .when(inventoryService).updateStock("INVALID", 200);

            // When/Then
            mockMvc.perform(put("/api/inventory/{skuCode}", "INVALID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 for negative quantity")
        void shouldReturn400ForNegativeQuantity() throws Exception {
            // Given
            InventoryRequest request = new InventoryRequest("SKU-001", -10);

            // When/Then
            mockMvc.perform(put("/api/inventory/{skuCode}", "SKU-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/inventory/{skuCode}/reserve")
    class ReserveStockTests {

        @Test
        @DisplayName("Should reserve stock successfully")
        void shouldReserveStockSuccessfully() throws Exception {
            // Given
            doNothing().when(inventoryService).reserveStock("SKU-001", 20, "ORDER-123");

            // When/Then
            mockMvc.perform(post("/api/inventory/{skuCode}/reserve", "SKU-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("quantity", "20")
                    .param("orderId", "ORDER-123"))
                    .andExpect(status().isOk());

            verify(inventoryService).reserveStock("SKU-001", 20, "ORDER-123");
        }

        @Test
        @DisplayName("Should return 409 when insufficient stock")
        void shouldReturn409WhenInsufficientStock() throws Exception {
            // Given
            doThrow(new InsufficientStockException("SKU-001", 100, 90))
                    .when(inventoryService).reserveStock("SKU-001", 100, "ORDER-123");

            // When/Then
            mockMvc.perform(post("/api/inventory/{skuCode}/reserve", "SKU-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("quantity", "100")
                    .param("orderId", "ORDER-123"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/inventory/{skuCode}/release")
    class ReleaseStockTests {

        @Test
        @DisplayName("Should release reserved stock successfully")
        void shouldReleaseReservedStockSuccessfully() throws Exception {
            // Given
            doNothing().when(inventoryService).releaseStock("SKU-001", 20, "ORDER-123");

            // When/Then
            mockMvc.perform(post("/api/inventory/{skuCode}/release", "SKU-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("quantity", "20")
                    .param("orderId", "ORDER-123"))
                    .andExpect(status().isOk());

            verify(inventoryService).releaseStock("SKU-001", 20, "ORDER-123");
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should return 400 for empty SKU code")
        void shouldReturn400ForEmptySkuCode() throws Exception {
            // Given
            InventoryRequest request = new InventoryRequest("", 100);

            // When/Then
            mockMvc.perform(put("/api/inventory/{skuCode}", "SKU-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for null quantity")
        void shouldReturn400ForNullQuantity() throws Exception {
            // When/Then
            mockMvc.perform(put("/api/inventory/{skuCode}", "SKU-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"skuCode\":\"SKU-001\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
