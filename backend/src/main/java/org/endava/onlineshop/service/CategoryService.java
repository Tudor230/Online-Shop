package org.endava.onlineshop.service;

import org.endava.onlineshop.model.dto.category.CategoryTreeNodeDto;
import org.endava.onlineshop.model.entities.Category;
import org.endava.onlineshop.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeNodeDto> getCategoryTree() {
        List<Category> all = categoryRepository.findAll();
        Map<UUID, List<Category>> childrenByParentId = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        return all.stream()
                .filter(c -> c.getParentId() == null)
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(c -> toTreeNode(c, childrenByParentId))
                .toList();
    }

    private CategoryTreeNodeDto toTreeNode(Category category, Map<UUID, List<Category>> childrenByParentId) {
        List<CategoryTreeNodeDto> children = childrenByParentId
                .getOrDefault(category.getId(), List.of())
                .stream()
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(c -> toTreeNode(c, childrenByParentId))
                .toList();

        return new CategoryTreeNodeDto(
                category.getId(),
                category.getParentId(),
                category.getName(),
                category.getSlug(),
                category.getPath(),
                children
        );
    }
}
