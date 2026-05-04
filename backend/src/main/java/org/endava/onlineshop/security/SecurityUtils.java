package org.endava.onlineshop.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SecurityUtils {

    public Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.of(jwtAuth.getToken());
        }
        return Optional.empty();
    }

    public Optional<UUID> getCurrentUserId() {
        return getCurrentJwt()
                .map(jwt -> jwt.getClaimAsString("sub"))
                .flatMap(sub -> {
                    try {
                        return Optional.of(UUID.fromString(sub));
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                });
    }

    public Optional<String> getCurrentUserEmail() {
        return getCurrentJwt()
                .map(jwt -> jwt.getClaimAsString("email"));
    }
}
