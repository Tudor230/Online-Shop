package org.endava.onlineshop.model.dto;

import java.time.Instant;
import java.util.UUID;
import org.endava.onlineshop.model.enums.Role;

public record UserResponseDto(
    UUID id,
    String email,
    String firstName,
    String lastName,
    Role role,
    UUID defaultShippingAddressId,
    UUID defaultBillingAddressId,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt) {}
