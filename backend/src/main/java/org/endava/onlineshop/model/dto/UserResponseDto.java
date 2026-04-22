package org.endava.onlineshop.model.dto;

import org.endava.onlineshop.model.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role,
        UUID defaultShippingAddressId,
        UUID defaultBillingAddressId,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

