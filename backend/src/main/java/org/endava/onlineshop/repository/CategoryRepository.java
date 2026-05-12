package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByParentIdAndSlug(UUID parentId, String slug);

    Optional<Category> findBySlug(String slug);
}

