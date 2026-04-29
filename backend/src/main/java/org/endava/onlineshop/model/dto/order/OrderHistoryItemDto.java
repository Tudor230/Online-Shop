package org.endava.onlineshop.model.dto.order;

import java.math.BigDecimal;

public record OrderHistoryItemDto(
        String productId,
        String title,
        String imagePlaceholder,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}

