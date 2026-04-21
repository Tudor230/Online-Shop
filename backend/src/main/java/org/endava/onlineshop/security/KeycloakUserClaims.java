package org.endava.onlineshop.security;

import org.endava.onlineshop.model.enums.Role;

public record KeycloakUserClaims(
        String email,
        String firstName,
        String lastName,
        Role role,
        Boolean isActive
) {
}

