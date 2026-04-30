package org.endava.onlineshop.security;

import org.endava.onlineshop.model.entities.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Objects;

public class UserAuthenticationToken extends AbstractAuthenticationToken {

    private final User user;

    public UserAuthenticationToken(User user, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.user = Objects.requireNonNull(user, "Authenticated user must not be null");
        setAuthenticated(true);
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

