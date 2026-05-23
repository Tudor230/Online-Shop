package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByOrderNumberAndUserId(String orderNumber, UUID userId);

    boolean existsByUserId(UUID userId);

    long countByCurrentStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    BigDecimal sumTotalAmount();

    @Query("""
            SELECT CAST(o.createdAt AS java.time.LocalDate), SUM(o.totalAmount)
            FROM Order o
            WHERE o.createdAt >= :from AND o.createdAt < :to
            GROUP BY CAST(o.createdAt AS java.time.LocalDate)
            ORDER BY CAST(o.createdAt AS java.time.LocalDate)
            """)
    List<Object[]> findRevenueBetween(@Param("from") Instant from, @Param("to") Instant to);

    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("SELECT o FROM Order o WHERE o.currentStatus = :status AND o.createdAt <= :cutoff")
    List<Order> findByStatusCreatedBeforeWithItems(@Param("status") OrderStatus status, @Param("cutoff") Instant cutoff);

    java.util.Optional<Order> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    default List<Order> findPendingOrdersCreatedBefore(Instant cutoff) {
        return findByStatusCreatedBeforeWithItems(OrderStatus.PENDING, cutoff);
    }

    @Query("""
            SELECT count(oi) > 0
            FROM Order o
            JOIN o.items oi
            WHERE o.userId = :userId
              AND oi.product.id = :productId
              AND o.currentStatus IN :eligibleStatuses
            """)
    boolean hasPurchasedProduct(
            @Param("userId") UUID userId,
            @Param("productId") UUID productId,
            @Param("eligibleStatuses") Collection<OrderStatus> eligibleStatuses
    );
}

