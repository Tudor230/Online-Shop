package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.admin.*;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.model.enums.Role;
import org.endava.onlineshop.repository.UserRepository;
import org.endava.onlineshop.security.KeycloakAdminService;
import org.endava.onlineshop.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final AdminAuditLogService auditLogService;
    private final SecurityUtils securityUtils;

    public AdminUserService(UserRepository userRepository,
                            KeycloakAdminService keycloakAdminService,
                            AdminAuditLogService auditLogService,
                            SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.auditLogService = auditLogService;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserListDto> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toListDto);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDto getUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String email = user.getEmail();
        Boolean isActive = user.getIsActive();

        try {
            java.util.Map<String, Object> kcUser = keycloakAdminService.getUserDetails(id);
            if (kcUser.containsKey("firstName") && kcUser.get("firstName") != null) {
                firstName = kcUser.get("firstName").toString();
            }
            if (kcUser.containsKey("lastName") && kcUser.get("lastName") != null) {
                lastName = kcUser.get("lastName").toString();
            }
            if (kcUser.containsKey("email") && kcUser.get("email") != null) {
                email = kcUser.get("email").toString();
            }
            if (kcUser.containsKey("enabled") && kcUser.get("enabled") != null) {
                isActive = (Boolean) kcUser.get("enabled");
            }
        } catch (Exception e) {
            // Fallback to local DB values if Keycloak fetch fails
        }

        return new AdminUserDetailDto(
                user.getId(), email, firstName, lastName,
                user.getRole(), isActive, user.getDefaultShippingAddressId(),
                user.getDefaultBillingAddressId(), user.getCreatedAt(), user.getUpdatedAt()
        );
    }

    @Transactional
    public AdminUserDetailDto createUser(AdminUserCreateRequestDto request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BadRequestException("User with this email already exists");
        }

        Role role = request.role() != null ? request.role() : Role.CUSTOMER;
        UUID keycloakUserId = keycloakAdminService.createUser(
                request.email(),
                request.firstName(),
                request.lastName(),
                request.password()
        );

        User user = new User();
        user.setId(keycloakUserId);
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(role);
        user.setIsActive(true);

        User saved = userRepository.save(user);
        audit("CREATE", "USER", saved.getId().toString(), "Created user " + saved.getEmail());
        return toDetailDto(saved);
    }

    @Transactional
    public AdminUserDetailDto updateUser(UUID id, AdminUserUpdateRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.firstName() != null) {
            String firstName = request.firstName().trim();
            user.setFirstName(firstName);
        }

        if (request.lastName() != null) {
            String lastName = request.lastName().trim();
            user.setLastName(lastName);
        }

        if (request.firstName() != null || request.lastName() != null) {
            keycloakAdminService.updateUserNames(id, user.getFirstName(), user.getLastName());
        }

        if (request.role() != null) {
            user.setRole(request.role());
        }

        if (request.isActive() != null) {
            user.setIsActive(request.isActive());
            keycloakAdminService.updateUserEnabled(id, request.isActive());
        }

        User saved = userRepository.save(user);
        audit("UPDATE", "USER", saved.getId().toString(), "Updated user " + saved.getEmail());
        return toDetailDto(saved);
    }

    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        keycloakAdminService.deleteUser(id);
        audit("DELETE", "USER", id.toString(), "Deleted user " + id);
        userRepository.deleteById(id);
    }

    private void audit(String action, String entityType, String entityId, String details) {
        auditLogService.log(
                securityUtils.getCurrentUserId().orElse(null),
                securityUtils.getCurrentUserEmail().orElse("system"),
                action, entityType, entityId, details
        );
    }

    @Transactional
    public java.util.Map<String, Integer> syncUsersFromKeycloak() {
        int created = 0;
        int updated = 0;

        java.util.List<java.util.Map<String, Object>> kcUsers = keycloakAdminService.getAllUsers();
        for (java.util.Map<String, Object> kcUser : kcUsers) {
            String kcId = (String) kcUser.get("id");
            String email = (String) kcUser.get("email");
            String firstName = (String) kcUser.get("firstName");
            String lastName = (String) kcUser.get("lastName");
            Boolean enabled = (Boolean) kcUser.get("enabled");

            if (kcId == null || email == null) continue;

            UUID userId = UUID.fromString(kcId);

            java.util.Optional<User> existing = userRepository.findById(userId);
            if (existing.isPresent()) {
                User user = existing.get();
                user.setEmail(email);
                user.setFirstName(firstName != null ? firstName : "");
                user.setLastName(lastName != null ? lastName : "");
                user.setIsActive(enabled != null ? enabled : true);
                userRepository.save(user);
                updated++;
            } else {
                User user = new User();
                user.setId(userId);
                user.setEmail(email);
                user.setFirstName(firstName != null ? firstName : "");
                user.setLastName(lastName != null ? lastName : "");
                user.setRole(Role.CUSTOMER);
                user.setIsActive(enabled != null ? enabled : true);
                userRepository.save(user);
                created++;
            }
        }

        return java.util.Map.of("created", created, "updated", updated);
    }

    private AdminUserListDto toListDto(User user) {
        return new AdminUserListDto(
                user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getRole(), user.getIsActive(), user.getCreatedAt()
        );
    }

    private AdminUserDetailDto toDetailDto(User user) {
        return new AdminUserDetailDto(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole(), user.getIsActive(), user.getDefaultShippingAddressId(),
                user.getDefaultBillingAddressId(), user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}
