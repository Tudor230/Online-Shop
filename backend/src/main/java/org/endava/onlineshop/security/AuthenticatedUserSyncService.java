package org.endava.onlineshop.security;

import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
public class AuthenticatedUserSyncService {

    private final ConcurrentHashMap<UUID, Object> userLocks = new ConcurrentHashMap<>();
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
        Object lock = userLocks.computeIfAbsent(keycloakUserId, ignored -> new Object());

        synchronized (lock) {
            User user = userRepository.findById(keycloakUserId).orElseGet(() -> {
                User newUser = new User();
                newUser.setId(keycloakUserId);
                return newUser;
            });
            applyClaims(user, claims, keycloakUserId);

            try {
                return userRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException ex) {
                User existingUser = userRepository.findById(keycloakUserId).orElseThrow(() -> ex);
                applyClaims(existingUser, claims, keycloakUserId);
                return userRepository.save(existingUser);
            } finally {
                userLocks.remove(keycloakUserId, lock);
            }
        }
    }

    private void applyClaims(User user, KeycloakUserClaims claims, UUID keycloakUserId) {
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

