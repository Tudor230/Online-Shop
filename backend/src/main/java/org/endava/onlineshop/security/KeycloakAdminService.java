package org.endava.onlineshop.security;

import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class KeycloakAdminService {

    private static final Set<String> MANAGED_REALM_ROLES = Set.of("ADMIN", "SUPPORT", "CUSTOMER");

    private final RestClient restClient;

    @Value("${keycloak.admin.server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.admin.realm}")
    private String keycloakRealm;

    @Value("${keycloak.admin.client-id}")
    private String keycloakAdminClientId;

    @Value("${keycloak.admin.client-secret}")
    private String keycloakAdminClientSecret;

    public KeycloakAdminService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public UUID createUser(String email, String firstName, String lastName, String password, Role role) {
        String accessToken = obtainAdminAccessToken();
        String createUserUri = "%s/admin/realms/%s/users".formatted(keycloakServerUrl, keycloakRealm);
        URI validatedUri = validateAbsoluteUri(createUserUri, "create-user");

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(validatedUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "username", email,
                            "email", email,
                            "firstName", firstName,
                            "lastName", lastName,
                            "enabled", true,
                            "emailVerified", true,
                            "credentials", java.util.List.of(Map.of(
                                    "type", "password",
                                    "value", password,
                                    "temporary", false
                            ))
                    ))
                    .retrieve()
                    .toBodilessEntity();

            UUID keycloakUserId = extractCreatedUserId(response, email, accessToken);
            syncUserRole(keycloakUserId, role, accessToken);
            return keycloakUserId;
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Failed to create user in Keycloak: " + ex.getMessage());
        }
    }

    public void deleteUser(UUID userId) {
        String accessToken = obtainAdminAccessToken();
        String deleteUserUri = "%s/admin/realms/%s/users/%s".formatted(keycloakServerUrl, keycloakRealm, userId);
        URI validatedUri = validateAbsoluteUri(deleteUserUri, "delete-user");

        try {
            restClient.delete()
                    .uri(validatedUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return;
            }
            throw new BadRequestException("Failed to delete user from Keycloak: " + ex.getMessage());
        }
    }

    public void updateUserNames(UUID userId, String firstName, String lastName) {
        String accessToken = obtainAdminAccessToken();
        updateUserDetails(userId, Map.of(
                "firstName", firstName,
                "lastName", lastName
        ), accessToken);
    }

    public void updateUserEnabled(UUID userId, boolean enabled) {
        String accessToken = obtainAdminAccessToken();
        updateUserDetails(userId, Map.of("enabled", enabled), accessToken);
    }

    public void syncUserRole(UUID userId, Role role) {
        String accessToken = obtainAdminAccessToken();
        syncUserRole(userId, role, accessToken);
    }

    private void syncUserRole(UUID userId, Role role, String accessToken) {
        Role desiredRole = role != null ? role : Role.CUSTOMER;
        Set<String> currentRoles = getUserRealmRoleNames(userId, accessToken).stream()
                .filter(MANAGED_REALM_ROLES::contains)
                .collect(java.util.stream.Collectors.toSet());

        Set<String> desiredRoles = Set.of(desiredRole.name());
        Set<String> toRemove = new HashSet<>(currentRoles);
        toRemove.removeAll(desiredRoles);
        Set<String> toAdd = new HashSet<>(desiredRoles);
        toAdd.removeAll(currentRoles);

        if (!toRemove.isEmpty()) {
            removeRealmRoles(userId, toRemove, accessToken);
        }
        if (!toAdd.isEmpty()) {
            addRealmRoles(userId, toAdd, accessToken);
        }
    }

    private void updateUserDetails(UUID userId, Map<String, Object> updates, String accessToken) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        String updateUserUri = "%s/admin/realms/%s/users/%s".formatted(keycloakServerUrl, keycloakRealm, userId);
        URI validatedUri = validateAbsoluteUri(updateUserUri, "update-user");

        try {
            restClient.put()
                    .uri(validatedUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(updates)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Failed to sync profile update to Keycloak");
        }
    }

    private String obtainAdminAccessToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", keycloakAdminClientId);
        formData.add("client_secret", keycloakAdminClientSecret);
        String tokenUri = "%s/realms/%s/protocol/openid-connect/token".formatted(keycloakServerUrl, keycloakRealm);
        URI validatedUri = validateAbsoluteUri(tokenUri, "token");

        try {
            Map<?, ?> response = restClient.post()
                    .uri(validatedUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !(response.get("access_token") instanceof String accessToken) || accessToken.isBlank()) {
                throw new BadRequestException("Failed to obtain Keycloak admin token");
            }

            return accessToken;
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Failed to obtain Keycloak admin token");
        }
    }

    private UUID extractCreatedUserId(ResponseEntity<Void> response, String email, String accessToken) {
        if (response != null) {
            URI location = response.getHeaders().getLocation();
            UUID locationId = parseUuidFromLocation(location);
            if (locationId != null) {
                return locationId;
            }
        }

        Optional<UUID> lookup = findUserIdByUsername(email, accessToken);
        if (lookup.isPresent()) {
            return lookup.get();
        }

        throw new BadRequestException("Failed to resolve Keycloak user id after creation");
    }

    private UUID parseUuidFromLocation(URI location) {
        if (location == null) {
            return null;
        }

        String path = location.getPath();
        if (path == null || path.isBlank()) {
            return null;
        }

        String id = path.substring(path.lastIndexOf('/') + 1);
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Optional<UUID> findUserIdByUsername(String username, String accessToken) {
        String usersUri = "%s/admin/realms/%s/users".formatted(keycloakServerUrl, keycloakRealm);
        URI baseUri = validateAbsoluteUri(usersUri, "user-search");
        URI searchUri = UriComponentsBuilder.fromUri(baseUri)
                .queryParam("username", username)
                .queryParam("exact", true)
                .build(true)
                .toUri();

        try {
            List<Map<String, Object>> users = restClient.get()
                    .uri(searchUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(List.class);

            if (users == null || users.isEmpty()) {
                return Optional.empty();
            }

            Object idValue = users.get(0).get("id");
            if (idValue instanceof String id) {
                try {
                    return Optional.of(UUID.fromString(id));
                } catch (IllegalArgumentException ex) {
                    return Optional.empty();
                }
            }

            return Optional.empty();
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Failed to lookup Keycloak user: " + ex.getMessage());
        }
    }

    private Set<String> getUserRealmRoleNames(UUID userId, String accessToken) {
        String rolesUri = "%s/admin/realms/%s/users/%s/role-mappings/realm".formatted(keycloakServerUrl, keycloakRealm, userId);
        URI validatedUri = validateAbsoluteUri(rolesUri, "user-role-mappings");

        try {
            List<Map<String, Object>> roles = restClient.get()
                    .uri(validatedUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(List.class);

            if (roles == null) {
                return Set.of();
            }

            Set<String> roleNames = new HashSet<>();
            for (Map<String, Object> role : roles) {
                Object name = role.get("name");
                if (name instanceof String roleName && !roleName.isBlank()) {
                    roleNames.add(roleName.toUpperCase());
                }
            }
            return roleNames;
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Failed to fetch Keycloak user roles: " + ex.getMessage());
        }
    }

    private void addRealmRoles(UUID userId, Collection<String> roleNames, String accessToken) {
        List<Map<String, Object>> roles = fetchRealmRoles(roleNames, accessToken);
        if (roles.isEmpty()) {
            return;
        }

        String roleMappingUri = "%s/admin/realms/%s/users/%s/role-mappings/realm".formatted(keycloakServerUrl, keycloakRealm, userId);
        URI validatedUri = validateAbsoluteUri(roleMappingUri, "add-user-roles");

        try {
            restClient.post()
                    .uri(validatedUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(roles)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Failed to assign Keycloak roles: " + ex.getMessage());
        }
    }

    private void removeRealmRoles(UUID userId, Collection<String> roleNames, String accessToken) {
        List<Map<String, Object>> roles = fetchRealmRoles(roleNames, accessToken);
        if (roles.isEmpty()) {
            return;
        }

        String roleMappingUri = "%s/admin/realms/%s/users/%s/role-mappings/realm".formatted(keycloakServerUrl, keycloakRealm, userId);
        URI validatedUri = validateAbsoluteUri(roleMappingUri, "remove-user-roles");

        try {
            restClient.method(HttpMethod.DELETE)
                    .uri(validatedUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(roles)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Failed to remove Keycloak roles: " + ex.getMessage());
        }
    }

    private List<Map<String, Object>> fetchRealmRoles(Collection<String> roleNames, String accessToken) {
        List<Map<String, Object>> roles = new ArrayList<>();
        for (String roleName : roleNames) {
            roles.add(fetchRealmRole(roleName, accessToken));
        }
        return roles;
    }

    private Map<String, Object> fetchRealmRole(String roleName, String accessToken) {
        String roleUri = "%s/admin/realms/%s/roles/%s".formatted(keycloakServerUrl, keycloakRealm, roleName);
        URI validatedUri = validateAbsoluteUri(roleUri, "realm-role");

        try {
            Map<String, Object> role = restClient.get()
                    .uri(validatedUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (role == null) {
                throw new BadRequestException("Keycloak role not found: " + roleName);
            }
            return role;
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Failed to fetch Keycloak role: " + roleName);
        }
    }

    private URI validateAbsoluteUri(String rawUri, String context) {
        try {
            URI uri = URI.create(rawUri);
            if (uri.getScheme() == null || uri.getScheme().isBlank()) {
                throw new IllegalArgumentException("URI scheme missing");
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid Keycloak " + context + " URI: '" + rawUri + "'. Check keycloak.admin.server-url and keycloak.admin.realm.");
        }
    }
}
