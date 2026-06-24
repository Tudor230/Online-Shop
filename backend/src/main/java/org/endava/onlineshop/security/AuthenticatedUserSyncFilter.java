package org.endava.onlineshop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.endava.onlineshop.model.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthenticatedUserSyncFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthenticatedUserSyncFilter.class);

    private final AuthenticatedUserSyncService authenticatedUserSyncService;

    public AuthenticatedUserSyncFilter(AuthenticatedUserSyncService authenticatedUserSyncService) {
        this.authenticatedUserSyncService = authenticatedUserSyncService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            try {
                User user = authenticatedUserSyncService.syncUser(jwtAuthenticationToken.getToken());
                UserAuthenticationToken userAuth = new UserAuthenticationToken(
                        user, jwtAuthenticationToken.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(userAuth);
            } catch (AuthenticationException ex) {
                log.warn("Authenticated request rejected because user sync failed", ex);
                SecurityContextHolder.clearContext();
                response.sendError(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
                return;
            } catch (RuntimeException ex) {
                log.error("Authenticated request rejected because user sync failed unexpectedly", ex);
                SecurityContextHolder.clearContext();
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication sync failed");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}


