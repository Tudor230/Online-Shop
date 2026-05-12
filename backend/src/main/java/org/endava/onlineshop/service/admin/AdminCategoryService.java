package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.events.CategoryPathChangedEvent;
import org.endava.onlineshop.events.ProductCategoriesChangedEvent;
import org.endava.onlineshop.model.dto.admin.AdminCategoryCreateRequestDto;
import org.endava.onlineshop.model.dto.admin.AdminCategoryDto;
import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.repository.CategoryRepository;
import org.endava.onlineshop.repository.ProductRepository;
import org.endava.onlineshop.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AdminAuditLogService auditLogService;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    public AdminCategoryService(CategoryRepository categoryRepository,
                                 ProductRepository productRepository,
                                 AdminAuditLogService auditLogService,
                                 SecurityUtils securityUtils,
                                 ApplicationEventPublisher eventPublisher) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
        this.securityUtils = securityUtils;
        this.eventPublisher = eventPublisher;
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
        Category category = new Category();
        category.setParentId(request.parentId());
        category.setName(request.name());
        category.setSlug(request.slug());

        Category saved = categoryRepository.save(category);
        audit("CREATE", "CATEGORY", saved.getId().toString(), "Created category " + saved.getName());
        eventPublisher.publishEvent(new CategoryPathChangedEvent(saved.getId()));
        return toDto(saved);
    }

    @Transactional
    public AdminCategoryDto updateCategory(UUID id, AdminCategoryCreateRequestDto request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        boolean pathChanged = false;

        if (request.slug() != null) {
            String slug = request.slug().trim();
            if (!slug.equals(category.getSlug())) {
                category.setSlug(slug);
                pathChanged = true;
            }
        }
        if (request.name() != null) category.setName(request.name());
        if (request.parentId() != null && !request.parentId().equals(category.getParentId())) {
            category.setParentId(request.parentId());
            pathChanged = true;
        }

        Category saved = categoryRepository.save(category);
        audit("UPDATE", "CATEGORY", saved.getId().toString(), "Updated category " + saved.getName());
        if (pathChanged) {
            eventPublisher.publishEvent(new CategoryPathChangedEvent(saved.getId()));
        }
        return toDto(saved);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }

        List<UUID> productIds = collectProductIdsForCategory(id);

        audit("DELETE", "CATEGORY", id.toString(), "Deleted category " + id);
        categoryRepository.deleteById(id);

        productIds.forEach(productId ->
                eventPublisher.publishEvent(new ProductCategoriesChangedEvent(productId)));
    }

    private void audit(String action, String entityType, String entityId, String details) {
        auditLogService.log(
                securityUtils.getCurrentUserId().orElse(null),
                securityUtils.getCurrentUserEmail().orElse("system"),
                action, entityType, entityId, details
        );
    }

    private List<UUID> collectProductIdsForCategory(UUID categoryId) {
        List<UUID> ids = new java.util.ArrayList<>();
        int page = 0;
        while (true) {
            Page<UUID> productIds = productRepository.findProductIdsByCategoryId(
                    categoryId,
                    PageRequest.of(page, 100)
            );
            if (productIds.isEmpty()) {
                break;
            }
            ids.addAll(productIds.getContent());
            if (!productIds.hasNext()) {
                break;
            }
            page++;
        }
        return ids;
    }

    private AdminCategoryDto toDto(Category category) {
        return new AdminCategoryDto(category.getId(), category.getParentId(), category.getName(), category.getSlug());
    }
}
