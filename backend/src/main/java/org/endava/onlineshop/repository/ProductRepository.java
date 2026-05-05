package org.endava.onlineshop.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.endava.onlineshop.model.entities.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

  @EntityGraph(attributePaths = {"categories", "inventory"})
  List<Product> findByIsActiveTrueOrderByNameAsc();

  @EntityGraph(attributePaths = {"categories", "inventory"})
  Optional<Product> findBySlugAndIsActiveTrue(String slug);

  @EntityGraph(attributePaths = {"categories", "inventory"})
  Optional<Product> findBySlug(String slug);

  boolean existsBySlug(String slug);

  boolean existsByIdAndIsActiveTrue(UUID id);

  boolean existsBySlugAndIsActiveTrue(String slug);
}
