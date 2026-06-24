package org.endava.onlineshop.controller;

import org.endava.onlineshop.model.dto.category.CategoryTreeNodeDto;
import org.endava.onlineshop.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryTreeNodeDto> getCategoryTree() {
        return categoryService.getCategoryTree();
    }
}
