package org.endava.onlineshop.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

@Component
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  private static final String ROLE_PREFIX = "ROLE_";
  private final JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter =
      new JwtGrantedAuthoritiesConverter();

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Set<GrantedAuthority> authorities =
        new LinkedHashSet<>(defaultAuthoritiesConverter.convert(jwt));
    authorities.addAll(extractRealmRoles(jwt));
    authorities.addAll(extractClientRoles(jwt));
    return authorities;
  }

  private Set<GrantedAuthority> extractRealmRoles(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess == null) {
      return Set.of();
    }
    return toAuthorities(realmAccess.get("roles"));
  }

  private Set<GrantedAuthority> extractClientRoles(Jwt jwt) {
    Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
    if (resourceAccess == null) {
      return Set.of();
    }

    Set<GrantedAuthority> clientAuthorities = new LinkedHashSet<>();
    for (Object clientAccess : resourceAccess.values()) {
      if (!(clientAccess instanceof Map<?, ?> clientAccessMap)) {
        continue;
      }
      clientAuthorities.addAll(toAuthorities(clientAccessMap.get("roles")));
    }
    return clientAuthorities;
  }

  private Set<GrantedAuthority> toAuthorities(Object rolesClaim) {
    if (!(rolesClaim instanceof Collection<?> roles)) {
      return Set.of();
    }

    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    for (Object role : roles) {
      if (!(role instanceof String roleName) || roleName.isBlank()) {
        continue;
      }
      String authority = roleName.startsWith(ROLE_PREFIX) ? roleName : ROLE_PREFIX + roleName;
      authorities.add(new SimpleGrantedAuthority(authority));
    }
    return authorities;
  }
}
