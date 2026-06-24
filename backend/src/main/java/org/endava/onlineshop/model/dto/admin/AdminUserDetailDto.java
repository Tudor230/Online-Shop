package org.endava.onlineshop.model.dto.admin;

import org.endava.onlineshop.model.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record AdminUserDetailDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role,
        Boolean isActive,
        UUID defaultShippingAddressId,
        UUID defaultBillingAddressId,
        Instant createdAt,
        Instant updatedAt
) {
}
