package org.endava.onlineshop.controller.admin;

import org.endava.onlineshop.model.dto.admin.*;
import org.endava.onlineshop.service.admin.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public Page<AdminUserListDto> getUsers(Pageable pageable) {
        return adminUserService.getUsers(pageable);
    }

    @GetMapping("/{id}")
    public AdminUserDetailDto getUser(@PathVariable UUID id) {
        return adminUserService.getUser(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserDetailDto createUser(@RequestBody AdminUserCreateRequestDto request) {
        return adminUserService.createUser(request);
    }

    @PutMapping("/{id}")
    public AdminUserDetailDto updateUser(@PathVariable UUID id, @RequestBody AdminUserUpdateRequestDto request) {
        return adminUserService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        adminUserService.deleteUser(id);
    }
}
