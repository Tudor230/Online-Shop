package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, UUID> {

    Optional<ProductInventory> findByProductId(UUID productId);

    @Query("SELECT pi FROM ProductInventory pi WHERE pi.quantityAvailable <= pi.lowStockThreshold")
    List<ProductInventory> findLowStockItems();

    @Query("SELECT pi FROM ProductInventory pi WHERE pi.quantityAvailable = 0")
    List<ProductInventory> findOutOfStockItems();
}
