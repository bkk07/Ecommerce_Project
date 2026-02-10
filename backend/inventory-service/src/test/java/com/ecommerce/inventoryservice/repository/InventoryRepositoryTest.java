package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.model.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("InventoryRepository Integration Tests")
class InventoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InventoryRepository inventoryRepository;

    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        testInventory = new Inventory();
        testInventory.setSkuCode("SKU-001");
        testInventory.setQuantity(100);
        testInventory.setReservedQuantity(10);
    }

    @Nested
    @DisplayName("findBySkuCode Tests")
    class FindBySkuCodeTests {

        @Test
        @DisplayName("Should find inventory by SKU code")
        void shouldFindInventoryBySkuCode() {
            // Given
            entityManager.persistAndFlush(testInventory);

            // When
            Optional<Inventory> result = inventoryRepository.findBySkuCode("SKU-001");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getSkuCode()).isEqualTo("SKU-001");
            assertThat(result.get().getQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should return empty when SKU not found")
        void shouldReturnEmptyWhenSkuNotFound() {
            // When
            Optional<Inventory> result = inventoryRepository.findBySkuCode("INVALID");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("SKU code should be case-sensitive")
        void skuCodeShouldBeCaseSensitive() {
            // Given
            entityManager.persistAndFlush(testInventory);

            // When
            Optional<Inventory> result = inventoryRepository.findBySkuCode("sku-001");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findBySkuCodeIn Tests")
    class FindBySkuCodeInTests {

        @Test
        @DisplayName("Should find multiple inventories by SKU codes")
        void shouldFindMultipleInventoriesBySkuCodes() {
            // Given
            Inventory inventory2 = new Inventory();
            inventory2.setSkuCode("SKU-002");
            inventory2.setQuantity(50);
            inventory2.setReservedQuantity(5);

            entityManager.persistAndFlush(testInventory);
            entityManager.persistAndFlush(inventory2);

            // When
            List<Inventory> result = inventoryRepository.findBySkuCodeIn(List.of("SKU-001", "SKU-002"));

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Inventory::getSkuCode)
                    .containsExactlyInAnyOrder("SKU-001", "SKU-002");
        }

        @Test
        @DisplayName("Should return empty list when no SKUs match")
        void shouldReturnEmptyListWhenNoSkusMatch() {
            // When
            List<Inventory> result = inventoryRepository.findBySkuCodeIn(List.of("INVALID-1", "INVALID-2"));

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle partial matches")
        void shouldHandlePartialMatches() {
            // Given
            entityManager.persistAndFlush(testInventory);

            // When
            List<Inventory> result = inventoryRepository.findBySkuCodeIn(List.of("SKU-001", "INVALID"));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSkuCode()).isEqualTo("SKU-001");
        }
    }

    @Nested
    @DisplayName("CRUD Operations Tests")
    class CrudOperationsTests {

        @Test
        @DisplayName("Should save inventory")
        void shouldSaveInventory() {
            // When
            Inventory saved = inventoryRepository.save(testInventory);

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(entityManager.find(Inventory.class, saved.getId())).isNotNull();
        }

        @Test
        @DisplayName("Should update inventory")
        void shouldUpdateInventory() {
            // Given
            Inventory saved = entityManager.persistAndFlush(testInventory);

            // When
            saved.setQuantity(200);
            inventoryRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then
            Inventory updated = entityManager.find(Inventory.class, saved.getId());
            assertThat(updated.getQuantity()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should delete inventory")
        void shouldDeleteInventory() {
            // Given
            Inventory saved = entityManager.persistAndFlush(testInventory);
            Long id = saved.getId();

            // When
            inventoryRepository.deleteById(id);
            entityManager.flush();

            // Then
            assertThat(entityManager.find(Inventory.class, id)).isNull();
        }
    }

    @Nested
    @DisplayName("Constraint Tests")
    class ConstraintTests {

        @Test
        @DisplayName("SKU code should be unique")
        void skuCodeShouldBeUnique() {
            // Given
            entityManager.persistAndFlush(testInventory);

            Inventory duplicate = new Inventory();
            duplicate.setSkuCode("SKU-001");
            duplicate.setQuantity(50);
            duplicate.setReservedQuantity(0);

            // When/Then
            assertThatThrownBy(() -> {
                inventoryRepository.save(duplicate);
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }
    }
}
