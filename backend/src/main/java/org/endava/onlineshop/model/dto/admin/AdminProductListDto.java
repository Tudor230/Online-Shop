package org.endava.onlineshop.model.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminProductListDto(
        UUID id,
        String sku,
        String name,
        String slug,
        BigDecimal basePrice,
        Boolean isActive,
        Double rating,
        Integer reviewCount,
        String imagePlaceholder,
        Integer quantityAvailable,
        Integer lowStockThreshold,
        List<String> categories,
        Instant createdAt,
        Instant updatedAt
) {
}
