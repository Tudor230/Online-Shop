package org.endava.onlineshop.security;

import org.endava.onlineshop.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class KeycloakAdminService {

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

    public UUID createUser(String email, String firstName, String lastName, String password) {
        String accessToken = obtainAdminAccessToken();

        // Check if user already exists in Keycloak
        Optional<UUID> existingId = findUserIdByUsername(email, accessToken);
        if (existingId.isPresent()) {
            updateUserDetails(existingId.get(), Map.of(
                    "firstName", firstName,
                    "lastName", lastName
            ), accessToken);
            return existingId.get();
        }

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
                            "credentials", List.of(Map.of(
                                    "type", "password",
                                    "value", password,
                                    "temporary", false
                            ))
                    ))
                    .retrieve()
                    .toBodilessEntity();

            UUID keycloakUserId = extractCreatedUserId(response, email, accessToken);
            return keycloakUserId;
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Failed to create user in Keycloak: " + ex.getResponseBodyAsString());
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

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllUsers() {
        String accessToken = obtainAdminAccessToken();
        String usersUri = "%s/admin/realms/%s/users".formatted(keycloakServerUrl, keycloakRealm);
        URI validatedUri = validateAbsoluteUri(usersUri, "list-users");

        try {
            List<Map<String, Object>> users = restClient.get()
                    .uri(validatedUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(List.class);
            return users != null ? users : List.of();
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Failed to list Keycloak users: " + ex.getResponseBodyAsString());
        }
    }

    public Map<String, Object> getUserDetails(UUID userId) {
        String accessToken = obtainAdminAccessToken();
        String userUri = "%s/admin/realms/%s/users/%s".formatted(keycloakServerUrl, keycloakRealm, userId);
        URI validatedUri = validateAbsoluteUri(userUri, "get-user");

        try {
            Map<String, Object> user = restClient.get()
                    .uri(validatedUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (user == null) {
                throw new BadRequestException("Keycloak user not found: " + userId);
            }

            return user;
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Failed to fetch Keycloak user: " + ex.getResponseBodyAsString());
        }
    }

    public void updateUserEnabled(UUID userId, boolean enabled) {
        String accessToken = obtainAdminAccessToken();
        updateUserDetails(userId, Map.of("enabled", enabled), accessToken);
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
            throw new BadRequestException("Failed to sync profile update to Keycloak: " + ex.getResponseBodyAsString());
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
            throw new BadRequestException("Failed to obtain Keycloak admin token: " + ex.getResponseBodyAsString());
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
