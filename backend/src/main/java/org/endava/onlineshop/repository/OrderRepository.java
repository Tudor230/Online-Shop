package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

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
}

