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
        return toDetailDto(user);
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
                request.password(),
                role
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
            keycloakAdminService.syncUserRole(id, request.role());
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
