package org.endava.onlineshop.model.dto.wishlist;

import java.util.List;

public record WishlistResponseDto(
        List<WishlistItemDto> items,
        int totalItems
) {
}
