package org.endava.onlineshop.model.dto.product;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailsDto(
        String id,
        String category,
        String title,
        double rating,
        int reviewCount,
        boolean canReview,
        boolean hasReviewed,
        String reviewedReviewId,
        BigDecimal price,
        String description,
        String imageId,
        List<String> imageGalleryIds,
        List<ProductReviewDto> reviews
) {
}

