package org.endava.onlineshop.model.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminProductDetailDto(
        UUID id,
        String sku,
        String name,
        String slug,
        String description,
        BigDecimal basePrice,
        Boolean isActive,
        Double rating,
        Integer reviewCount,
        String imagePlaceholder,
        List<String> imageGallery,
        List<AdminCategoryDto> categories,
        AdminInventoryDto inventory,
        Instant createdAt,
        Instant updatedAt
) {
}
