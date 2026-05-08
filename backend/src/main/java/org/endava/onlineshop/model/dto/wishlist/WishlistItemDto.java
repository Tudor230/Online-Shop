package org.endava.onlineshop.model.dto.wishlist;

import java.math.BigDecimal;
import java.time.Instant;

public record WishlistItemDto(
        String productSlug,
        String title,
        BigDecimal price,
        String imageLabel,
        Instant createdAt
) {
}
