package org.endava.onlineshop.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    private final SecurityUtils securityUtils = new SecurityUtils();

    @Test
    void shouldReturnEmptyWhenNoAuthentication() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        Optional<Jwt> result = securityUtils.getCurrentJwt();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNotJwtAuthentication() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        Optional<Jwt> result = securityUtils.getCurrentJwt();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnJwtWhenAuthenticated() {
        UUID userId = UUID.randomUUID();
        JwtAuthenticationToken jwtAuth = mock(JwtAuthenticationToken.class);
        when(jwtAuth.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn(userId.toString());
        when(jwt.getClaimAsString("email")).thenReturn("admin@test.com");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(jwtAuth);

        Optional<Jwt> jwtResult = securityUtils.getCurrentJwt();
        assertThat(jwtResult).isPresent();

        Optional<UUID> userIdResult = securityUtils.getCurrentUserId();
        assertThat(userIdResult).isPresent().hasValue(userId);

        Optional<String> emailResult = securityUtils.getCurrentUserEmail();
        assertThat(emailResult).isPresent().hasValue("admin@test.com");
    }

    @Test
    void shouldReturnEmptyUserIdWhenInvalidJwt() {
        JwtAuthenticationToken jwtAuth = mock(JwtAuthenticationToken.class);
        when(jwtAuth.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn("invalid-uuid");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(jwtAuth);

        Optional<UUID> result = securityUtils.getCurrentUserId();
        assertThat(result).isEmpty();
    }
}
