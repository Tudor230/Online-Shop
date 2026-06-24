package org.endava.onlineshop.model.dto.admin;

import org.endava.onlineshop.model.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record AdminUserListDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role,
        Boolean isActive,
        Instant createdAt
) {
}
