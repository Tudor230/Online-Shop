package org.endava.onlineshop.model.dto.cart;

import java.util.List;

public record CartResponseDto(
        List<CartItemDto> items,
        Integer totalItems,
        java.math.BigDecimal subtotal,
        java.math.BigDecimal shippingAmount,
        java.math.BigDecimal taxAmount,
        java.math.BigDecimal totalAmount,
        String currencyCode
) {
}
