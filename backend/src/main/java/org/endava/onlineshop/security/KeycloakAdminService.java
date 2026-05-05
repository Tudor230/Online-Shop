package org.endava.onlineshop.security;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.endava.onlineshop.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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

  public void updateUserNames(UUID userId, String firstName, String lastName) {
    String accessToken = obtainAdminAccessToken();
    String updateUserUri =
        "%s/admin/realms/%s/users/%s".formatted(keycloakServerUrl, keycloakRealm, userId);
    URI validatedUri = validateAbsoluteUri(updateUserUri, "update-user");

    try {
      restClient
          .put()
          .uri(validatedUri)
          .header("Authorization", "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              Map.of(
                  "firstName", firstName,
                  "lastName", lastName))
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
    String tokenUri =
        "%s/realms/%s/protocol/openid-connect/token".formatted(keycloakServerUrl, keycloakRealm);
    URI validatedUri = validateAbsoluteUri(tokenUri, "token");

    try {
      Map<?, ?> response =
          restClient
              .post()
              .uri(validatedUri)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(formData)
              .retrieve()
              .body(Map.class);

      if (response == null
          || !(response.get("access_token") instanceof String accessToken)
          || accessToken.isBlank()) {
        throw new BadRequestException("Failed to obtain Keycloak admin token");
      }

      return accessToken;
    } catch (RestClientResponseException ex) {
      throw new BadRequestException("Failed to obtain Keycloak admin token");
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
      throw new BadRequestException(
          "Invalid Keycloak "
              + context
              + " URI: '"
              + rawUri
              + "'. Check keycloak.admin.server-url and keycloak.admin.realm.");
    }
  }
}
