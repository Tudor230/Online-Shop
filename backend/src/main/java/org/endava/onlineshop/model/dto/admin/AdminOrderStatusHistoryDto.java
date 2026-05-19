package org.endava.onlineshop.model.dto.admin;

import org.endava.onlineshop.model.enums.OrderStatus;

import java.time.Instant;

public record AdminOrderStatusHistoryDto(
        OrderStatus status,
        String notes,
        Instant createdAt
) {
}
