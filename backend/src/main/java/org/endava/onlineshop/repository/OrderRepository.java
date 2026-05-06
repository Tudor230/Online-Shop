package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserId(UUID userId);

    java.util.Optional<Order> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);
}

