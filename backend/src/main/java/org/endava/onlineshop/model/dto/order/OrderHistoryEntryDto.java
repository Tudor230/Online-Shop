package org.endava.onlineshop.model.dto.order;

import org.endava.onlineshop.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderHistoryEntryDto(
        String id,
        String orderNumber,
        OrderStatus status,
        Instant createdAt,
        BigDecimal subtotal,
        BigDecimal shippingAmount,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String currencyCode,
        List<OrderHistoryItemDto> items
) {
}

