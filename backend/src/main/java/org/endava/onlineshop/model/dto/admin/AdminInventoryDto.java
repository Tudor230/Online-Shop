package org.endava.onlineshop.model.dto.admin;

public record AdminInventoryDto(
        Integer quantityAvailable,
        Integer lowStockThreshold
) {
}
