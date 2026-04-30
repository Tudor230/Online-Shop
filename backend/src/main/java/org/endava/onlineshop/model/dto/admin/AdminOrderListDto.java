package org.endava.onlineshop.model.dto.admin;

import org.endava.onlineshop.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminOrderListDto(
        UUID id,
        String orderNumber,
        UUID userId,
        String guestEmail,
        OrderStatus currentStatus,
        BigDecimal totalAmount,
        Instant createdAt
) {
}
