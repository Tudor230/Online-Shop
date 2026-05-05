package org.endava.onlineshop.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.endava.onlineshop.model.entities.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
  @EntityGraph(attributePaths = {"product"})
  List<CartItem> findByCartIdOrderByCreatedAtAsc(UUID cartId);

  Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);
}
