package org.endava.onlineshop.service;

import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.wishlist.WishlistItemDto;
import org.endava.onlineshop.model.dto.wishlist.WishlistResponseDto;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.model.entities.WishlistItem;
import org.endava.onlineshop.repository.ProductRepository;
import org.endava.onlineshop.repository.WishlistItemRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;

    public WishlistService(WishlistItemRepository wishlistItemRepository, ProductRepository productRepository) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public WishlistResponseDto getWishlist(User user) {
        UUID userId = requireAuthenticatedUserId(user);
        return toResponse(userId);
    }

    @Transactional
    public WishlistResponseDto addItem(User user, String productId) {
        UUID userId = requireAuthenticatedUserId(user);
        Product product = productRepository.findBySlugAndIsActiveTrue(productId)
                .orElseThrow(() -> new BadRequestException("Product not found"));

        if (!wishlistItemRepository.existsByUserIdAndProductId(userId, product.getId())) {
            WishlistItem wishlistItem = new WishlistItem(userId, product.getId());
            try {
                wishlistItemRepository.saveAndFlush(wishlistItem);
            } catch (DataIntegrityViolationException ignored) {
                // Request is idempotent: another concurrent request already saved this row.
            }
        }

        return toResponse(userId);
    }

    @Transactional
    public WishlistResponseDto removeItem(User user, String productId) {
        UUID userId = requireAuthenticatedUserId(user);
        Product product = productRepository.findBySlug(productId)
                .orElseThrow(() -> new BadRequestException("Product not found"));

        wishlistItemRepository.deleteByUserIdAndProductId(userId, product.getId());
        return toResponse(userId);
    }

    private WishlistResponseDto toResponse(UUID userId) {
        List<WishlistItemDto> items = wishlistItemRepository.findItemViewsByUserId(userId).stream()
                .map(item -> new WishlistItemDto(
                        item.getProductSlug(),
                        item.getProductName(),
                        item.getProductPrice(),
                        item.getImageId(),
                        item.getAddedAt()
                ))
                .toList();
        return new WishlistResponseDto(items, items.size());
    }

    private UUID requireAuthenticatedUserId(User user) {
        if (user == null || user.getId() == null) {
            throw new BadRequestException("Authentication is required");
        }
        return user.getId();
    }
}
