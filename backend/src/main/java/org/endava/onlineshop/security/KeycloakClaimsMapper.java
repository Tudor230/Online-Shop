package org.endava.onlineshop.security;

import org.endava.onlineshop.model.enums.Role;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
public class KeycloakClaimsMapper {

    public KeycloakUserClaims toUserClaims(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");
        Boolean isActive = jwt.getClaimAsBoolean("email_verified");

        if (isBlank(firstName) || isBlank(lastName)) {
            String fullName = jwt.getClaimAsString("name");
            if (!isBlank(fullName)) {
                String[] parts = fullName.trim().split("\\s+", 2);
                if (isBlank(firstName) && parts.length > 0) {
                    firstName = parts[0];
                }
                if (isBlank(lastName) && parts.length > 1) {
                    lastName = parts[1];
                }
            }
        }

        return new KeycloakUserClaims(email, firstName, lastName, resolveRole(jwt), isActive != null ? isActive : true);
    }

    private Role resolveRole(Jwt jwt) {
        if (hasRole(jwt, "ADMIN")) {
            return Role.ADMIN;
        }
        if (hasRole(jwt, "SUPPORT")) {
            return Role.SUPPORT;
        }
        return Role.CUSTOMER;
    }

    private boolean hasRole(Jwt jwt, String expectedRole) {
        String normalizedExpected = expectedRole.toUpperCase();

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && containsRole(realmAccess.get("roles"), normalizedExpected)) {
            return true;
        }

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null) {
            return false;
        }

        for (Object clientAccess : resourceAccess.values()) {
            if (!(clientAccess instanceof Map<?, ?> clientAccessMap)) {
                continue;
            }
            if (containsRole(clientAccessMap.get("roles"), normalizedExpected)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsRole(Object rolesClaim, String expectedRole) {
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return false;
        }

        for (Object role : roles) {
            if (!(role instanceof String roleName) || roleName.isBlank()) {
                continue;
            }
            String normalized = roleName.toUpperCase();
            if (normalized.startsWith("ROLE_")) {
                normalized = normalized.substring("ROLE_".length());
            }
            if (normalized.equals(expectedRole)) {
                return true;
            }
        }

        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

