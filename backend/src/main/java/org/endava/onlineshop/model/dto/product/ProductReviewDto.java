package org.endava.onlineshop.model.dto.product;

import java.time.Instant;

public record ProductReviewDto(
        String id,
        int rating,
        String comment,
        String reviewerName,
        Instant createdAt
) {
}

