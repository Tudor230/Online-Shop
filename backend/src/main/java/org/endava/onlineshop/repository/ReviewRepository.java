package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    interface ProductReviewAggregate {
        UUID getProductId();

        Double getAverageRating();

        Long getReviewCount();
    }

    @EntityGraph(attributePaths = {"user"})
    List<Review> findByProductIdOrderByCreatedAtDesc(UUID productId);

    boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    Optional<Review> findByProductIdAndUserId(UUID productId, UUID userId);

    int countByProductId(UUID productId);

    @Query("""
            SELECT r.product.id AS productId,
                   AVG(r.rating) AS averageRating,
                   COUNT(r) AS reviewCount
            FROM Review r
            WHERE r.product.id IN :productIds
            GROUP BY r.product.id
            """)
    List<ProductReviewAggregate> summarizeByProductIds(@Param("productIds") Collection<UUID> productIds);
}

