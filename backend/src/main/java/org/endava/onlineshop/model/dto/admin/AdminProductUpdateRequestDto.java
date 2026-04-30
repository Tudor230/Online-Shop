package org.endava.onlineshop.model.dto.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AdminProductUpdateRequestDto(
        String sku,
        String name,
        String slug,
        String description,
        BigDecimal basePrice,
        Boolean isActive,
        List<UUID> categoryIds,
        String imagePlaceholder,
        List<String> imageGallery,
        Integer quantityAvailable,
        Integer lowStockThreshold
) {
}
