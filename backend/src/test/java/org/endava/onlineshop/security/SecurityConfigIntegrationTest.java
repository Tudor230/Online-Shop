package org.endava.onlineshop.security;

import org.junit.jupiter.api.Test;
import org.endava.onlineshop.model.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityTestController.class)
@Import({SecurityConfig.class, JwtRoleConverter.class, SecurityConfigIntegrationTest.TestBeans.class})
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private AuthenticatedUserSyncService authenticatedUserSyncService;

    @Autowired
    private JwtRoleConverter jwtRoleConverter;

    @Test
    void shouldSwapJwtPrincipalToUserPrincipalForAuthenticationPrincipalInjection() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000123");
        when(authenticatedUserSyncService.syncUser(any(Jwt.class))).thenReturn(userWithId(userId));

        mockMvc.perform(get("/test/current-user-id")
                        .with(jwt().jwt(token -> token.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().string(userId.toString()));
    }

    @Test
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/test/authenticated"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAnonymousAccessToProductEndpoints() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowAnonymousAccessToProductSearchViaQueryParameter() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "wireless mouse"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowAnonymousAccessToCartEndpoints() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowAnonymousAccessToCartItemModificationEndpoints() throws Exception {
        mockMvc.perform(patch("/api/cart/items/00000000-0000-0000-0000-000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectUnauthenticatedGuestCartClaim() throws Exception {
        mockMvc.perform(post("/api/cart/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Session-Id", "guest-session"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthenticatedRequests() throws Exception {
        when(authenticatedUserSyncService.syncUser(any(Jwt.class))).thenReturn(userWithId(UUID.randomUUID()));

        mockMvc.perform(get("/test/authenticated")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("authenticated"));
    }

    @Test
    void shouldAllowAdminEndpointWhenRealmRoleIsPresent() throws Exception {
        when(authenticatedUserSyncService.syncUser(any(Jwt.class))).thenReturn(userWithId(UUID.randomUUID()));

        mockMvc.perform(get("/test/admin")
                        .with(jwt()
                                .jwt(token -> token.claim("realm_access", Map.of("roles", List.of("ADMIN"))))
                                .authorities(jwtRoleConverter)))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));
    }

    @Test
    void shouldAllowAdminEndpointWhenClientRoleIsPresent() throws Exception {
        when(authenticatedUserSyncService.syncUser(any(Jwt.class))).thenReturn(userWithId(UUID.randomUUID()));

        mockMvc.perform(get("/test/admin")
                        .with(jwt()
                                .jwt(token -> token.claim("resource_access", Map.of(
                                        "shop-frontend", Map.of("roles", List.of("ADMIN"))
                                )))
                                .authorities(jwtRoleConverter)))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));
    }

    @Test
    void shouldReturnForbiddenWhenRequiredRoleIsMissing() throws Exception {
        when(authenticatedUserSyncService.syncUser(any(Jwt.class))).thenReturn(userWithId(UUID.randomUUID()));

        mockMvc.perform(get("/test/admin")
                        .with(jwt()
                                .jwt(token -> token.claim("realm_access", Map.of("roles", List.of("CUSTOMER"))))
                                .authorities(jwtRoleConverter)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserSyncFails() throws Exception {
        when(authenticatedUserSyncService.syncUser(any(Jwt.class)))
                .thenThrow(new BadCredentialsException("Invalid authentication subject"));

        mockMvc.perform(get("/test/authenticated")
                        .with(jwt()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserSyncReturnsNull() throws Exception {
        when(authenticatedUserSyncService.syncUser(any(Jwt.class))).thenReturn(null);

        mockMvc.perform(get("/test/authenticated")
                        .with(jwt()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowPostWithoutCsrfWhenAuthenticated() throws Exception {
        when(authenticatedUserSyncService.syncUser(any(Jwt.class))).thenReturn(userWithId(UUID.randomUUID()));

        mockMvc.perform(post("/test/echo")
                        .with(jwt())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("hello"));
    }

    private static User userWithId(UUID userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        AuthenticatedUserSyncFilter authenticatedUserSyncFilter(AuthenticatedUserSyncService authenticatedUserSyncService) {
            return new AuthenticatedUserSyncFilter(authenticatedUserSyncService);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test-user")
                    .build();
        }
    }
}
