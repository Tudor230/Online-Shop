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
            select wi.productId as productId,
                   p.slug as productSlug,
                   p.name as productName,
                   p.basePrice as productPrice,
                   p.imagePlaceholder as imagePlaceholder,
                   wi.addedAt as addedAt
            from WishlistItem wi
            join Product p on p.id = wi.productId
            where wi.userId = :userId
            order by wi.addedAt desc
            """)
    List<WishlistItemView> findItemViewsByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    void deleteByUserIdAndProductId(UUID userId, UUID productId);

    interface WishlistItemView {
        UUID getProductId();

        String getProductSlug();

        String getProductName();

        BigDecimal getProductPrice();

        String getImagePlaceholder();

        Instant getAddedAt();
    }
}
