package org.endava.onlineshop.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRoleConverterTest {

  private final JwtRoleConverter converter = new JwtRoleConverter();

  @Test
  void shouldExtractRealmAndClientRolesAlongWithScopes() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("scope", "read write")
            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
            .claim("resource_access", Map.of("shop-frontend", Map.of("roles", List.of("CUSTOMER"))))
            .build();

    Set<String> authorities =
        converter.convert(jwt).stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

    assertThat(authorities).contains("SCOPE_read", "SCOPE_write", "ROLE_ADMIN", "ROLE_CUSTOMER");
  }

  @Test
  void shouldNormalizeAndDeduplicateRoles() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("realm_access", Map.of("roles", List.of("ADMIN", "ROLE_ADMIN")))
            .claim(
                "resource_access",
                Map.of("shop-frontend", Map.of("roles", List.of("ADMIN", "SUPPORT"))))
            .build();

    Set<String> authorities =
        converter.convert(jwt).stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

    assertThat(authorities).contains("ROLE_ADMIN", "ROLE_SUPPORT");
    assertThat(authorities.stream().filter("ROLE_ADMIN"::equals).count()).isEqualTo(1);
  }

  @Test
  void shouldHandleMissingRoleClaims() {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "test-user").build();

    Set<String> authorities =
        converter.convert(jwt).stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

    assertThat(authorities).isEmpty();
  }
}
