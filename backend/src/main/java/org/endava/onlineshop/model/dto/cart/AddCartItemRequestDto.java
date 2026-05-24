package org.endava.onlineshop.model.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddCartItemRequestDto(
        @NotNull UUID productId,
        @NotNull @Min(1) Integer quantity
) {
}
