package org.endava.onlineshop.repository;

import java.util.List;
import java.util.UUID;
import org.endava.onlineshop.model.entities.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

  @EntityGraph(attributePaths = {"items", "items.product"})
  List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

  boolean existsByUserId(UUID userId);
}
