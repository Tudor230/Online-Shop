package org.endava.onlineshop.security;

import java.util.Collection;
import java.util.Objects;
import org.endava.onlineshop.model.entities.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

public class UserAuthenticationToken extends AbstractAuthenticationToken {

  private final User user;

  public UserAuthenticationToken(User user, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.user = Objects.requireNonNull(user, "Authenticated user must not be null");
    super.setAuthenticated(true);
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    Assert.isTrue(
        !isAuthenticated,
        "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
    super.setAuthenticated(false);
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public User getPrincipal() {
    return user;
  }
}
