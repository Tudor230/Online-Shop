package org.endava.onlineshop.model.dto.cart;

import java.util.List;

public record CartResponseDto(
        List<CartItemDto> items,
        Integer totalItems
) {
}
