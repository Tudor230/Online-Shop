package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, UUID> {
    Optional<ShoppingCart> findByUserId(UUID userId);

    Optional<ShoppingCart> findBySessionId(String sessionId);
}
