package org.endava.onlineshop.model.dto.admin;

import org.endava.onlineshop.model.enums.OrderStatus;

public record AdminOrderStatusUpdateDto(
        OrderStatus newStatus,
        String notes
) {
}
