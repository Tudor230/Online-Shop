package org.endava.onlineshop.security;

import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
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

    private final SecurityUtils securityUtils = new SecurityUtils();

    @Test
    void shouldReturnEmptyWhenNoAuthentication() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        Optional<User> result = securityUtils.getCurrentUser();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNotUserAuthentication() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        Optional<User> result = securityUtils.getCurrentUser();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnUserWhenAuthenticated() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("admin@test.com");
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setRole(Role.ADMIN);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        UserAuthenticationToken userAuth = new UserAuthenticationToken(user, authorities);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(userAuth);

        Optional<User> userResult = securityUtils.getCurrentUser();
        assertThat(userResult).isPresent();
        assertThat(userResult.get().getEmail()).isEqualTo("admin@test.com");

        Optional<UUID> userIdResult = securityUtils.getCurrentUserId();
        assertThat(userIdResult).isPresent().hasValue(userId);

        Optional<String> emailResult = securityUtils.getCurrentUserEmail();
        assertThat(emailResult).isPresent().hasValue("admin@test.com");
    }
}
