package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.WishlistItem;
import org.endava.onlineshop.model.entities.WishlistItemId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, WishlistItemId> {
    @Query("""
            SELECT wi.productId AS productId,
                   p.slug AS productSlug,
                   p.name AS productName,
                   p.basePrice AS productPrice,
                   p.imageId AS imageId,
                   wi.createdAt AS addedAt
              FROM WishlistItem wi
              JOIN Product p ON p.id = wi.productId
              WHERE wi.userId = :userId
              ORDER BY wi.createdAt DESC
             """)
    List<WishlistItemView> findItemViewsByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    void deleteByUserIdAndProductId(UUID userId, UUID productId);

    interface WishlistItemView {
        UUID getProductId();

        String getProductSlug();

        String getProductName();

        BigDecimal getProductPrice();

        String getImageId();

        Instant getAddedAt();
    }
}
