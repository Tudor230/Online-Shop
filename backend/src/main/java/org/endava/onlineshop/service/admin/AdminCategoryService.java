package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.admin.AdminCategoryCreateRequestDto;
import org.endava.onlineshop.model.dto.admin.AdminCategoryDto;
import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.repository.CategoryRepository;
import org.endava.onlineshop.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final AdminAuditLogService auditLogService;
    private final SecurityUtils securityUtils;

    public AdminCategoryService(CategoryRepository categoryRepository,
                                 AdminAuditLogService auditLogService,
                                 SecurityUtils securityUtils) {
        this.categoryRepository = categoryRepository;
        this.auditLogService = auditLogService;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public List<AdminCategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminCategoryDto getCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        return toDto(category);
    }

    @Transactional
    public AdminCategoryDto createCategory(AdminCategoryCreateRequestDto request) {
        if (categoryRepository.findBySlug(request.slug()).isPresent()) {
            throw new BadRequestException("Category slug already exists");
        }

        Category category = new Category();
        category.setParentId(request.parentId());
        category.setName(request.name());
        category.setSlug(request.slug());

        Category saved = categoryRepository.save(category);
        audit("CREATE", "CATEGORY", saved.getId().toString(), "Created category " + saved.getName());
        return toDto(saved);
    }

    @Transactional
    public AdminCategoryDto updateCategory(UUID id, AdminCategoryCreateRequestDto request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (request.slug() != null) {
            String slug = request.slug().trim();
            if (!slug.equals(category.getSlug())) {
                if (categoryRepository.findBySlug(slug).isPresent()) {
                    throw new BadRequestException("Category slug already exists");
                }
                category.setSlug(slug);
            }
        }
        if (request.name() != null) category.setName(request.name());
        if (request.parentId() != null) category.setParentId(request.parentId());

        Category saved = categoryRepository.save(category);
        audit("UPDATE", "CATEGORY", saved.getId().toString(), "Updated category " + saved.getName());
        return toDto(saved);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        audit("DELETE", "CATEGORY", id.toString(), "Deleted category " + id);
        categoryRepository.deleteById(id);
    }

    private void audit(String action, String entityType, String entityId, String details) {
        auditLogService.log(
                securityUtils.getCurrentUserId().orElse(null),
                securityUtils.getCurrentUserEmail().orElse("system"),
                action, entityType, entityId, details
        );
    }

    private AdminCategoryDto toDto(Category category) {
        return new AdminCategoryDto(category.getId(), category.getParentId(), category.getName(), category.getSlug());
    }
}
