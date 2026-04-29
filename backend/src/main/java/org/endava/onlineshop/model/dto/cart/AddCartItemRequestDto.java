package org.endava.onlineshop.model.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record AddCartItemRequestDto(
        @NotBlank String productId,
        @NotNull @Min(1) Integer quantity
) {
}
