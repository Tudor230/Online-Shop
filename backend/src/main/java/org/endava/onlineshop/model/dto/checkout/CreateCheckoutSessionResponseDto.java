package org.endava.onlineshop.model.dto.checkout;

import java.util.UUID;

public record CreateCheckoutSessionResponseDto(
        String checkoutUrl,
        UUID orderId
) {
}
