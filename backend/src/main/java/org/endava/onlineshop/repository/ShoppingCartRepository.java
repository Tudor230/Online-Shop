package org.endava.onlineshop.repository;

import java.util.Optional;
import java.util.UUID;
import org.endava.onlineshop.model.entities.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, UUID> {
  Optional<ShoppingCart> findByUserId(UUID userId);

  Optional<ShoppingCart> findBySessionId(String sessionId);
}
