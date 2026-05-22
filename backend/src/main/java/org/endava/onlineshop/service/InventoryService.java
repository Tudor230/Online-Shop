package org.endava.onlineshop.service;

import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.entities.OrderItem;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.ProductInventory;
import org.endava.onlineshop.repository.ProductInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductInventoryRepository productInventoryRepository;

    @Transactional
    public void reserveItems(List<OrderItem> items) {
        adjustInventory(items, -1, "reserve");
    }

    @Transactional
    public void releaseItems(List<OrderItem> items) {
        adjustInventory(items, 1, "release");
    }

    private void adjustInventory(List<OrderItem> items, int direction, String action) {
        if (items == null || items.isEmpty()) {
            return;
        }

        Map<UUID, Integer> quantities = new HashMap<>();
        for (OrderItem item : items) {
            Product product = item.getProduct();
            if (product == null || product.getId() == null) {
                throw new BadRequestException("Order item is missing product details");
            }
            if (item.getQuantity() == null || item.getQuantity() < 1) {
                throw new BadRequestException("Order item has invalid quantity");
            }
            quantities.merge(product.getId(), item.getQuantity(), Integer::sum);
        }

        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            UUID productId = entry.getKey();
            int quantity = entry.getValue();
            ProductInventory inventory = productInventoryRepository.findByProductIdForUpdate(productId)
                    .orElseThrow(() -> new BadRequestException("Inventory entry missing for product: " + productId));
            int available = inventory.getQuantityAvailable() != null ? inventory.getQuantityAvailable() : 0;
            int updated = available + (direction * quantity);
            if (direction < 0 && available < quantity) {
                throw new BadRequestException("Insufficient stock for product: " + productId);
            }
            if (updated < 0) {
                throw new BadRequestException("Cannot " + action + " inventory below zero for product: " + productId);
            }
            inventory.setQuantityAvailable(updated);
        }
    }
}

