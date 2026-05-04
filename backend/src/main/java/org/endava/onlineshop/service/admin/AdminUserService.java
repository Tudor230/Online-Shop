package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.admin.*;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.model.enums.Role;
import org.endava.onlineshop.repository.UserRepository;
import org.endava.onlineshop.security.KeycloakAdminService;
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

    public AdminUserService(UserRepository userRepository, KeycloakAdminService keycloakAdminService) {
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
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

        UUID userId = UUID.randomUUID();
        keycloakAdminService.createUser(userId, request.email(), request.firstName(), request.lastName(), request.password());

        User user = new User();
        user.setId(userId);
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(request.role() != null ? request.role() : Role.CUSTOMER);
        user.setIsActive(true);

        User saved = userRepository.save(user);
        return toDetailDto(saved);
    }

    @Transactional
    public AdminUserDetailDto updateUser(UUID id, AdminUserUpdateRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.firstName() != null) {
            String firstName = request.firstName().trim();
            user.setFirstName(firstName);
            if (request.lastName() != null) {
                String lastName = request.lastName().trim();
                user.setLastName(lastName);
                keycloakAdminService.updateUserNames(id, firstName, lastName);
            } else {
                keycloakAdminService.updateUserNames(id, firstName, user.getLastName());
            }
        } else if (request.lastName() != null) {
            String lastName = request.lastName().trim();
            user.setLastName(lastName);
            keycloakAdminService.updateUserNames(id, user.getFirstName(), lastName);
        }

        if (request.role() != null) user.setRole(request.role());
        if (request.isActive() != null) user.setIsActive(request.isActive());

        User saved = userRepository.save(user);
        return toDetailDto(saved);
    }

    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        keycloakAdminService.deleteUser(id);
        userRepository.deleteById(id);
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
