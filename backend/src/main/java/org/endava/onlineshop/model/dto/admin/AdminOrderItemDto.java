package org.endava.onlineshop.model.dto.admin;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminOrderItemDto(
        UUID productId,
        String productName,
        String productImage,
        Integer quantity,
        BigDecimal unitPriceAtPurchase
) {
}
