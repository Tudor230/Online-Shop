package org.endava.onlineshop.controller;

import org.endava.onlineshop.model.dto.wishlist.WishlistResponseDto;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.service.WishlistService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public WishlistResponseDto getWishlist(@AuthenticationPrincipal User user) {
        return wishlistService.getWishlist(user);
    }

    @PostMapping("/items/{productSlug}")
    public WishlistResponseDto addItem(@AuthenticationPrincipal User user, @PathVariable String productSlug) {
        return wishlistService.addItem(user, productSlug);
    }

    @DeleteMapping("/items/{productSlug}")
    public WishlistResponseDto removeItem(@AuthenticationPrincipal User user, @PathVariable String productSlug) {
        return wishlistService.removeItem(user, productSlug);
    }
}
