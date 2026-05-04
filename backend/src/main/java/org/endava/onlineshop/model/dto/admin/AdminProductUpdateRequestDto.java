package org.endava.onlineshop.model.dto.admin;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AdminProductUpdateRequestDto(
        @Size(max = 50) String sku,
        @Size(max = 200) String name,
        @Size(max = 220) String slug,
        String description,
        @Positive BigDecimal basePrice,
        Boolean isActive,
        List<UUID> categoryIds,
        @Size(max = 255) String imagePlaceholder,
        List<String> imageGallery,
        @PositiveOrZero Integer quantityAvailable,
        @PositiveOrZero Integer lowStockThreshold
) {
}
