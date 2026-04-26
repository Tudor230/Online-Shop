package org.endava.onlineshop.model.dto.product;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailsDto(
        String id,
        String category,
        String title,
        double rating,
        int reviewCount,
        BigDecimal price,
        String description,
        String imagePlaceholder,
        List<String> imageGallery
) {
}

