package org.endava.onlineshop.model.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.endava.onlineshop.model.enums.OrderStatus;

public record OrderHistoryEntryDto(
    String id,
    String orderNumber,
    OrderStatus status,
    Instant createdAt,
    BigDecimal subtotal,
    BigDecimal discountAmount,
    BigDecimal totalAmount,
    List<OrderHistoryItemDto> items) {}
