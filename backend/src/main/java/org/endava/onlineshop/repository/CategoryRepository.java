package org.endava.onlineshop.repository;

import java.util.Optional;
import java.util.UUID;
import org.endava.onlineshop.model.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
  Optional<Category> findBySlug(String slug);
}
