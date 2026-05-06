package org.endava.onlineshop.model.dto.checkout;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCheckoutSessionRequestDto(
        @NotNull(message = "Shipping address id is required")
        UUID shippingAddressId,

        @NotNull(message = "Billing address id is required")
        UUID billingAddressId
) {
}
