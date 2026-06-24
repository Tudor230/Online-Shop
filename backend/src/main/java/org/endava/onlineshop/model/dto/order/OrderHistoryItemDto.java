package org.endava.onlineshop.model.dto.order;

import java.math.BigDecimal;

public record OrderHistoryItemDto(
        String productSlug,
        String category,
        String title,
        String imageId,
        String description,
        double productRating,
        int productReviewCount,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        boolean canLeaveReview,
        boolean reviewed,
        String reviewId,
        Integer reviewedRating
) {
}

