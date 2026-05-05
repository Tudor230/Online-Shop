package org.endava.onlineshop.controller;

import jakarta.validation.Valid;
import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.cart.AddCartItemRequestDto;
import org.endava.onlineshop.model.dto.cart.CartResponseDto;
import org.endava.onlineshop.model.dto.cart.UpdateCartItemQuantityRequestDto;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

  private final CartService cartService;

  public CartController(CartService cartService) {
    this.cartService = cartService;
  }

  @GetMapping
  public CartResponseDto getCart(
      @AuthenticationPrincipal User user,
      @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
    return cartService.getCart(user, sessionId);
  }

  @PostMapping("/items")
  public CartResponseDto addItem(
      @AuthenticationPrincipal User user,
      @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
      @Valid @RequestBody AddCartItemRequestDto request) {
    return cartService.addItem(user, sessionId, request);
  }

  @PatchMapping("/items/{productId}")
  public CartResponseDto updateItemQuantity(
      @AuthenticationPrincipal User user,
      @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
      @PathVariable String productId,
      @Valid @RequestBody UpdateCartItemQuantityRequestDto request) {
    return cartService.updateItemQuantity(user, sessionId, productId, request.quantity());
  }

  @DeleteMapping("/items/{productId}")
  public CartResponseDto removeItem(
      @AuthenticationPrincipal User user,
      @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
      @PathVariable String productId) {
    return cartService.removeItem(user, sessionId, productId);
  }

  @PostMapping("/claim")
  public CartResponseDto claimGuestCart(
      @AuthenticationPrincipal User user, @RequestHeader(value = "X-Session-Id") String sessionId) {
    if (user == null) {
      throw new BadRequestException("Authentication is required");
    }
    return cartService.claimGuestCart(user, sessionId);
  }
}
