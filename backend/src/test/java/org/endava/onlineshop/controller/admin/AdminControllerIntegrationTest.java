package org.endava.onlineshop.controller.admin;

import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.security.AuthenticatedUserSyncFilter;
import org.endava.onlineshop.security.AuthenticatedUserSyncService;
import org.endava.onlineshop.security.JwtRoleConverter;
import org.endava.onlineshop.security.SecurityConfig;
import org.endava.onlineshop.service.admin.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        AdminDashboardController.class,
        AdminProductController.class,
        AdminOrderController.class,
        AdminUserController.class,
        AdminCategoryController.class,
        AdminAuditLogController.class
})
@Import({SecurityConfig.class, JwtRoleConverter.class, AdminControllerIntegrationTest.TestBeans.class})
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtRoleConverter jwtRoleConverter;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @MockBean
    private AdminProductService adminProductService;

    @MockBean
    private AdminOrderService adminOrderService;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private AdminCategoryService adminCategoryService;

    @MockBean
    private AdminAuditLogService adminAuditLogService;

    @MockBean
    private AuthenticatedUserSyncService authenticatedUserSyncService;

    @BeforeEach
    void setUp() {
        when(authenticatedUserSyncService.syncUser(any(Jwt.class))).thenReturn(
                userWithId(UUID.randomUUID())
        );
    }

    // =================== Dashboard ===================

    @Test
    void shouldAllowAdminToGetDashboardStats() throws Exception {
        when(adminDashboardService.getStats()).thenReturn(
                new org.endava.onlineshop.model.dto.admin.AdminDashboardStatsDto(
                        10L, 5L, 20L, 2L, 3L, new BigDecimal("1000.50")
                )
        );

        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(10))
                .andExpect(jsonPath("$.totalUsers").value(5))
                .andExpect(jsonPath("$.totalProducts").value(20));
    }

    @Test
    void shouldAllowSupportToGetDashboardStats() throws Exception {
        when(adminDashboardService.getStats()).thenReturn(
                new org.endava.onlineshop.model.dto.admin.AdminDashboardStatsDto(
                        10L, 5L, 20L, 2L, 3L, new BigDecimal("1000.50")
                )
        );

        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .with(supportJwt()))
                .andExpect(status().isOk());
    }

    // =================== Products ===================

    @Test
    void shouldAllowAdminToGetProducts() throws Exception {
        when(adminProductService.getProducts(any())).thenReturn(
                new PageImpl<>(List.of())
        );

        mockMvc.perform(get("/api/admin/products")
                        .with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowSupportToGetProducts() throws Exception {
        when(adminProductService.getProducts(any())).thenReturn(
                new PageImpl<>(List.of())
        );

        mockMvc.perform(get("/api/admin/products")
                        .with(supportJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAdminToCreateProduct() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminProductService.createProduct(any())).thenReturn(
                new org.endava.onlineshop.model.dto.admin.AdminProductDetailDto(
                        id, "SKU-001", "Test Product", "test-product", "Description",
                        new BigDecimal("99.99"), true, 0.0, 0, "placeholder",
                        List.of(), List.of(),
                        new org.endava.onlineshop.model.dto.admin.AdminInventoryDto(10, 5),
                        null, null
                )
        );

        mockMvc.perform(post("/api/admin/products")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "sku": "SKU-001",
                                    "name": "Test Product",
                                    "slug": "test-product",
                                    "basePrice": 99.99,
                                    "imagePlaceholder": "placeholder"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectProductCreationBySupport() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .with(supportJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "sku": "SKU-001",
                                    "name": "Test Product",
                                    "slug": "test-product",
                                    "basePrice": 99.99,
                                    "imagePlaceholder": "placeholder"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    // =================== Orders ===================

    @Test
    void shouldAllowAdminToGetOrders() throws Exception {
        when(adminOrderService.getOrders(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/orders")
                        .with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowSupportToGetOrders() throws Exception {
        when(adminOrderService.getOrders(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/orders")
                        .with(supportJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectOrderStatusUpdateBySupport() throws Exception {
        mockMvc.perform(put("/api/admin/orders/" + UUID.randomUUID() + "/status")
                        .with(supportJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newStatus": "SHIPPED"}
                                """))
                .andExpect(status().isForbidden());
    }

    // =================== Users ===================

    @Test
    void shouldAllowAdminToGetUsers() throws Exception {
        when(adminUserService.getUsers(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/users")
                        .with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAdminToCreateUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminUserService.createUser(any())).thenReturn(
                new org.endava.onlineshop.model.dto.admin.AdminUserDetailDto(
                        id,
                        "admin@test.com",
                        "Admin",
                        "User",
                        org.endava.onlineshop.model.enums.Role.ADMIN,
                        true,
                        null,
                        null,
                        null,
                        null
                )
        );

        mockMvc.perform(post("/api/admin/users")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@test.com",
                                  "firstName": "Admin",
                                  "lastName": "User",
                                  "role": "ADMIN",
                                  "password": "Admin123!"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldAllowAdminToUpdateUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminUserService.updateUser(any(), any())).thenReturn(
                new org.endava.onlineshop.model.dto.admin.AdminUserDetailDto(
                        id,
                        "admin@test.com",
                        "Admin",
                        "User",
                        org.endava.onlineshop.model.enums.Role.SUPPORT,
                        false,
                        null,
                        null,
                        null,
                        null
                )
        );

        mockMvc.perform(put("/api/admin/users/" + id)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Admin",
                                  "lastName": "User",
                                  "role": "SUPPORT",
                                  "isActive": false
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAdminToDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/admin/users/" + UUID.randomUUID())
                        .with(adminJwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectUserAccessBySupport() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(supportJwt()))
                .andExpect(status().isForbidden());
    }

    // =================== Categories ===================

    @Test
    void shouldAllowAdminToGetCategories() throws Exception {
        when(adminCategoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/categories")
                        .with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowSupportToGetCategories() throws Exception {
        when(adminCategoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/categories")
                        .with(supportJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectCategoryCreationBySupport() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                        .with(supportJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Test", "slug": "test"}
                                """))
                .andExpect(status().isForbidden());
    }

    // =================== Audit Logs ===================

    @Test
    void shouldAllowAdminToGetAuditLogs() throws Exception {
        when(adminAuditLogService.getAllLogs(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .with(adminJwt()))
                .andExpect(status().isOk());
    }

    // =================== Unauthenticated / Unauthorized ===================

    @Test
    void shouldRejectUnauthenticatedAdminRequests() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectCustomerRoleOnAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .with(customerJwt()))
                .andExpect(status().isForbidden());
    }

    // =================== Helper Methods ===================

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return jwt()
                .jwt(token -> token.claim("realm_access", Map.of("roles", List.of("ADMIN"))))
                .authorities(jwtRoleConverter);
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor supportJwt() {
        return jwt()
                .jwt(token -> token.claim("realm_access", Map.of("roles", List.of("SUPPORT"))))
                .authorities(jwtRoleConverter);
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor customerJwt() {
        return jwt()
                .jwt(token -> token.claim("realm_access", Map.of("roles", List.of("CUSTOMER"))))
                .authorities(jwtRoleConverter);
    }

    private static User userWithId(UUID userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("admin@test.com");
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
            return token -> org.springframework.security.oauth2.jwt.Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test-user")
                    .build();
        }
    }
}
