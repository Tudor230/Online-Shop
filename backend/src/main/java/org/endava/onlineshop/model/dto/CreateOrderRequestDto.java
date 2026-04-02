package org.endava.onlineshop.model.dto;


import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequestDto(
        @NotNull(message = "userId must not be null") Long userId,
        @NotNull(message = "productIds must not be null") List<Long> productIds) {
}
