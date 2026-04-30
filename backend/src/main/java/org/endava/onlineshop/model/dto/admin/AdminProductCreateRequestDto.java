package org.endava.onlineshop.model.dto.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AdminProductCreateRequestDto(
        String sku,
        String name,
        String slug,
        String description,
        BigDecimal basePrice,
        List<UUID> categoryIds,
        String imagePlaceholder,
        List<String> imageGallery,
        Integer initialQuantity,
        Integer lowStockThreshold
) {
}
