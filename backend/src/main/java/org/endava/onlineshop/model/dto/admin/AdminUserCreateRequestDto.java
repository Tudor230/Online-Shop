package org.endava.onlineshop.model.dto.admin;

import org.endava.onlineshop.model.enums.Role;

public record AdminUserCreateRequestDto(
        String email,
        String firstName,
        String lastName,
        Role role,
        String password
) {
}
