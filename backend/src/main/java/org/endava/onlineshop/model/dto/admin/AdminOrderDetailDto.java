package org.endava.onlineshop.model.dto.admin;

import org.endava.onlineshop.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminOrderDetailDto(
        UUID id,
        String orderNumber,
        UUID userId,
        String guestEmail,
        OrderStatus currentStatus,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        UUID shippingAddressId,
        UUID billingAddressId,
        List<AdminOrderItemDto> items,
        List<AdminOrderStatusHistoryDto> statusHistory,
        Instant createdAt,
        Instant updatedAt
) {
}
