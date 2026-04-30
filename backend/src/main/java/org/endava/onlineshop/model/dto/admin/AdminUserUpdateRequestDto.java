package org.endava.onlineshop.model.dto.admin;

import org.endava.onlineshop.model.enums.Role;

public record AdminUserUpdateRequestDto(
        String firstName,
        String lastName,
        Role role,
        Boolean isActive
) {
}
