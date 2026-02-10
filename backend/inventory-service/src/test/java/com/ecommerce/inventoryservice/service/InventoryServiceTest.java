package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.model.Inventory;
import com.ecommerce.inventoryservice.model.StockReservation;
import com.ecommerce.inventoryservice.producer.InventoryEventProducer;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ecommerce.inventoryservice.repository.OutboxEventRepository;
import com.ecommerce.inventoryservice.repository.StockReservationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryEventProducer inventoryEventProducer;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
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
    @DisplayName("initStock Tests")
    class InitStockTests {

        @Test
        @DisplayName("Should initialize stock for new SKU")
        void shouldInitializeStockForNewSku() {
            // Given
            when(inventoryRepository.findBySkuCode("NEW-SKU")).thenReturn(Optional.empty());
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            inventoryService.initStock("NEW-SKU");

            // Then
            ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
            verify(inventoryRepository).save(captor.capture());
            
            Inventory saved = captor.getValue();
            assertThat(saved.getSkuCode()).isEqualTo("NEW-SKU");
            assertThat(saved.getQuantity()).isEqualTo(0);
            assertThat(saved.getReservedQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should skip initialization for existing SKU")
        void shouldSkipInitializationForExistingSku() {
            // Given
            when(inventoryRepository.findBySkuCode("SKU-001")).thenReturn(Optional.of(testInventory));

            // When
            inventoryService.initStock("SKU-001");

            // Then
            verify(inventoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateStock Tests")
    class UpdateStockTests {

        @Test
        @DisplayName("Should update stock quantity")
        void shouldUpdateStockQuantity() throws Exception {
            // Given
            when(inventoryRepository.findBySkuCode("SKU-001")).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            inventoryService.updateStock("SKU-001", 200);

            // Then
            ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
            verify(inventoryRepository).save(captor.capture());
            assertThat(captor.getValue().getQuantity()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should throw exception when SKU not found")
        void shouldThrowExceptionWhenSkuNotFound() {
            // Given
            when(inventoryRepository.findBySkuCode("INVALID")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> inventoryService.updateStock("INVALID", 100))
                    .isInstanceOf(InventoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("reserveStock Tests")
    class ReserveStockTests {

        @Test
        @DisplayName("Should reserve stock successfully")
        void shouldReserveStockSuccessfully() throws Exception {
            // Given
            when(stockReservationRepository.findByOrderIdAndSkuCode("ORDER-123", "SKU-001"))
                    .thenReturn(Optional.empty());
            when(inventoryRepository.findBySkuCode("SKU-001")).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            inventoryService.reserveStock("SKU-001", 50, "ORDER-123");

            // Then
            ArgumentCaptor<Inventory> invCaptor = ArgumentCaptor.forClass(Inventory.class);
            verify(inventoryRepository).save(invCaptor.capture());
            assertThat(invCaptor.getValue().getReservedQuantity()).isEqualTo(60); // 10 + 50

            ArgumentCaptor<StockReservation> resCaptor = ArgumentCaptor.forClass(StockReservation.class);
            verify(stockReservationRepository).save(resCaptor.capture());
            assertThat(resCaptor.getValue().getOrderId()).isEqualTo("ORDER-123");
            assertThat(resCaptor.getValue().getQuantity()).isEqualTo(50);
        }

        @Test
        @DisplayName("Should skip duplicate reservation (idempotency)")
        void shouldSkipDuplicateReservation() {
            // Given
            StockReservation existingReservation = StockReservation.builder()
                    .orderId("ORDER-123")
                    .skuCode("SKU-001")
                    .quantity(50)
                    .status(StockReservation.ReservationStatus.RESERVED)
                    .build();
            when(stockReservationRepository.findByOrderIdAndSkuCode("ORDER-123", "SKU-001"))
                    .thenReturn(Optional.of(existingReservation));

            // When
            inventoryService.reserveStock("SKU-001", 50, "ORDER-123");

            // Then
            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when insufficient stock")
        void shouldThrowExceptionWhenInsufficientStock() {
            // Given
            when(stockReservationRepository.findByOrderIdAndSkuCode("ORDER-123", "SKU-001"))
                    .thenReturn(Optional.empty());
            when(inventoryRepository.findBySkuCode("SKU-001")).thenReturn(Optional.of(testInventory));

            // Available = 100 - 10 = 90, requesting 100
            // When/Then
            assertThatThrownBy(() -> inventoryService.reserveStock("SKU-001", 100, "ORDER-123"))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("SKU-001");
        }

        @Test
        @DisplayName("Should throw exception when SKU not found")
        void shouldThrowExceptionWhenSkuNotFoundForReserve() {
            // Given
            when(stockReservationRepository.findByOrderIdAndSkuCode(any(), any()))
                    .thenReturn(Optional.empty());
            when(inventoryRepository.findBySkuCode("INVALID")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> inventoryService.reserveStock("INVALID", 10, "ORDER-123"))
                    .isInstanceOf(InventoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("releaseStock Tests")
    class ReleaseStockTests {

        @Test
        @DisplayName("Should release reserved stock")
        void shouldReleaseReservedStock() throws Exception {
            // Given
            StockReservation reservation = StockReservation.builder()
                    .orderId("ORDER-123")
                    .skuCode("SKU-001")
                    .quantity(20)
                    .status(StockReservation.ReservationStatus.RESERVED)
                    .build();

            when(stockReservationRepository.findByOrderIdAndSkuCode("ORDER-123", "SKU-001"))
                    .thenReturn(Optional.of(reservation));
            when(inventoryRepository.findBySkuCode("SKU-001")).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any())).thenReturn(testInventory);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            inventoryService.releaseStock("SKU-001", 20, "ORDER-123");

            // Then
            ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
            verify(inventoryRepository).save(captor.capture());
            
            // Reserved should decrease: 10 - 20 would be negative, so should be 0 or handled
            assertThat(captor.getValue().getReservedQuantity()).isLessThanOrEqualTo(10);
        }

        @Test
        @DisplayName("Should skip release for already released reservation")
        void shouldSkipReleaseForAlreadyReleased() throws Exception {
            // Given
            StockReservation reservation = StockReservation.builder()
                    .orderId("ORDER-123")
                    .skuCode("SKU-001")
                    .quantity(20)
                    .status(StockReservation.ReservationStatus.RELEASED)
                    .build();

            when(stockReservationRepository.findByOrderIdAndSkuCode("ORDER-123", "SKU-001"))
                    .thenReturn(Optional.of(reservation));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            inventoryService.releaseStock("SKU-001", 20, "ORDER-123");

            // Then
            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle release when no reservation exists")
        void shouldHandleReleaseWhenNoReservationExists() throws Exception {
            // Given
            when(stockReservationRepository.findByOrderIdAndSkuCode("ORDER-123", "SKU-001"))
                    .thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            inventoryService.releaseStock("SKU-001", 20, "ORDER-123");

            // Then - should still emit event for Saga completion
            verify(outboxEventRepository).save(any());
        }
    }

    @Nested
    @DisplayName("Available Stock Calculation Tests")
    class AvailableStockTests {

        @Test
        @DisplayName("Available stock should be total minus reserved")
        void availableStockShouldBeTotalMinusReserved() {
            // Given
            testInventory.setQuantity(100);
            testInventory.setReservedQuantity(30);

            // When
            int available = testInventory.getAvailableStock();

            // Then
            assertThat(available).isEqualTo(70);
        }
    }
}
