package org.endava.onlineshop.model.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AdminProductCreateRequestDto(
        @NotNull @Size(max = 50) String sku,
        @NotNull @Size(max = 200) String name,
        @NotNull @Size(max = 220) String slug,
        String description,
        @NotNull @Positive BigDecimal basePrice,
        List<UUID> categoryIds,
        @NotNull @Size(max = 255) String imagePlaceholder,
        List<String> imageGallery,
        @PositiveOrZero Integer initialQuantity,
        @PositiveOrZero Integer lowStockThreshold
) {
}
