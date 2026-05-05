package org.endava.onlineshop.model.dto.product;

import java.math.BigDecimal;

public record ProductSummaryDto(
        String id,
        String category,
        String title,
        double rating,
        int reviewCount,
        BigDecimal price,
        String imageId
) {
}

