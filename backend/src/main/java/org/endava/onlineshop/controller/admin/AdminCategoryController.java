package org.endava.onlineshop.controller.admin;

import jakarta.validation.Valid;
import org.endava.onlineshop.model.dto.admin.AdminCategoryCreateRequestDto;
import org.endava.onlineshop.model.dto.admin.AdminCategoryDto;
import org.endava.onlineshop.service.admin.AdminCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT')")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    public AdminCategoryController(AdminCategoryService adminCategoryService) {
        this.adminCategoryService = adminCategoryService;
    }

    @GetMapping
    public List<AdminCategoryDto> getCategories() {
        return adminCategoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public AdminCategoryDto getCategory(@PathVariable UUID id) {
        return adminCategoryService.getCategory(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AdminCategoryDto createCategory(@Valid @RequestBody AdminCategoryCreateRequestDto request) {
        return adminCategoryService.createCategory(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminCategoryDto updateCategory(@PathVariable UUID id, @Valid @RequestBody AdminCategoryCreateRequestDto request) {
        return adminCategoryService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCategory(@PathVariable UUID id) {
        adminCategoryService.deleteCategory(id);
    }
}
