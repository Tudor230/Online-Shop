package org.endava.onlineshop.model.dto;


import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequestDto(
        @NotNull(message = "userId must not be null") UUID userId,
        @NotNull(message = "productIds must not be null") List<Long> productIds) {
}
