package org.endava.onlineshop.security;

import org.endava.onlineshop.model.entities.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SecurityUtils {

    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UserAuthenticationToken userAuth) {
            return Optional.of(userAuth.getPrincipal());
        }
        return Optional.empty();
    }

    public Optional<UUID> getCurrentUserId() {
        return getCurrentUser()
                .map(User::getId);
    }

    public Optional<String> getCurrentUserEmail() {
        return getCurrentUser()
                .map(User::getEmail);
    }
}
