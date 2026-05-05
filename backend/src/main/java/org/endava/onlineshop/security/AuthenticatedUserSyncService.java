package org.endava.onlineshop.security;

import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public User syncUser(Jwt jwt) {
        UUID keycloakUserId = parseKeycloakId(jwt.getSubject());

        KeycloakUserClaims claims = keycloakClaimsMapper.toUserClaims(jwt);

        User user = userRepository.findById(keycloakUserId).orElse(null);
        boolean isNewUser = user == null;
        if (isNewUser) {
            user = new User();
            user.setId(keycloakUserId);
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

        return userRepository.save(user);
    }

    private UUID parseKeycloakId(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new BadCredentialsException("Invalid authentication subject");
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid authentication subject");
        }
    }

    private String nonBlankOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}


