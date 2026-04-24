package org.endava.onlineshop.security;

import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthenticatedUserSyncService {

    private final UserRepository userRepository;
    private final KeycloakClaimsMapper keycloakClaimsMapper;

    public AuthenticatedUserSyncService(UserRepository userRepository, KeycloakClaimsMapper keycloakClaimsMapper) {
        this.userRepository = userRepository;
        this.keycloakClaimsMapper = keycloakClaimsMapper;
    }

    @Transactional
    public void syncUser(Jwt jwt) {
        UUID keycloakUserId = parseKeycloakId(jwt.getSubject());
        if (keycloakUserId == null) {
            return;
        }

        KeycloakUserClaims claims = keycloakClaimsMapper.toUserClaims(jwt);

        User user = userRepository.findById(keycloakUserId).orElse(null);
        boolean isNewUser = user == null;
        if (isNewUser) {
            user = new User();
            user.setId(keycloakUserId);
            user.setCreatedAt(LocalDateTime.now());
        }

        // Sync account bootstrap details from Keycloak to DB.
        // Names are user-managed in profile update flow and must not be overwritten on every request.
        user.setEmail(nonBlankOrFallback(claims.email(), keycloakUserId + "@keycloak.local"));
        if (isBlank(user.getFirstName())) {
            user.setFirstName(nonBlankOrFallback(claims.firstName(), "Unknown"));
        }
        if (isBlank(user.getLastName())) {
            user.setLastName(nonBlankOrFallback(claims.lastName(), "User"));
        }
        user.setRole(claims.role());
        user.setIsActive(claims.isActive());

        userRepository.save(user);
    }

    private UUID parseKeycloakId(String subject) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String nonBlankOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}



