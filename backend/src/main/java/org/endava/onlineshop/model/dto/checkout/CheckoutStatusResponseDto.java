package org.endava.onlineshop.model.dto.checkout;

import org.endava.onlineshop.model.enums.OrderStatus;

import java.util.UUID;

public record CheckoutStatusResponseDto(
        UUID orderId,
        OrderStatus status
) {
}
