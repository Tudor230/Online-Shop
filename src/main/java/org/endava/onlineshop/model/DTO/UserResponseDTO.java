package org.endava.onlineshop.model.dto;

import org.endava.onlineshop.model.enums.Role;

public record UserResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        Role role
) {
}
