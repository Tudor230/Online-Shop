package org.endava.onlineshop.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityTestController.class)
@Import({SecurityConfig.class, JwtRoleConverter.class, SecurityConfigIntegrationTest.TestBeans.class})
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtRoleConverter jwtRoleConverter;

    @Test
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/test/authenticated"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthenticatedRequests() throws Exception {
        mockMvc.perform(get("/test/authenticated")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("authenticated"));
    }

    @Test
    void shouldAllowAdminEndpointWhenRealmRoleIsPresent() throws Exception {
        mockMvc.perform(get("/test/admin")
                        .with(jwt()
                                .jwt(token -> token.claim("realm_access", Map.of("roles", List.of("ADMIN"))))
                                .authorities(jwtRoleConverter)))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));
    }

    @Test
    void shouldAllowAdminEndpointWhenClientRoleIsPresent() throws Exception {
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
        mockMvc.perform(get("/test/admin")
                        .with(jwt()
                                .jwt(token -> token.claim("realm_access", Map.of("roles", List.of("CUSTOMER"))))
                                .authorities(jwtRoleConverter)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowPostWithoutCsrfWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/test/echo")
                        .with(jwt())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("hello"));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test-user")
                    .build();
        }
    }
}
