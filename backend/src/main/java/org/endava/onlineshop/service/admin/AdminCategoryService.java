package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.admin.AdminCategoryCreateRequestDto;
import org.endava.onlineshop.model.dto.admin.AdminCategoryDto;
import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;

    public AdminCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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
        return toDto(saved);
    }

    @Transactional
    public AdminCategoryDto updateCategory(UUID id, AdminCategoryCreateRequestDto request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (request.slug() != null && !request.slug().equals(category.getSlug())) {
            if (categoryRepository.findBySlug(request.slug()).isPresent()) {
                throw new BadRequestException("Category slug already exists");
            }
            category.setSlug(request.slug());
        }
        if (request.name() != null) category.setName(request.name());
        if (request.parentId() != null) category.setParentId(request.parentId());

        Category saved = categoryRepository.save(category);
        return toDto(saved);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        categoryRepository.deleteById(id);
    }

    private AdminCategoryDto toDto(Category category) {
        return new AdminCategoryDto(category.getId(), category.getParentId(), category.getName(), category.getSlug());
    }
}
