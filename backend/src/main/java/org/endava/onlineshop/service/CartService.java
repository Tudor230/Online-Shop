package org.endava.onlineshop.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.cart.AddCartItemRequestDto;
import org.endava.onlineshop.model.dto.cart.CartItemDto;
import org.endava.onlineshop.model.dto.cart.CartResponseDto;
import org.endava.onlineshop.model.entities.CartItem;
import org.endava.onlineshop.model.entities.Product;
import org.endava.onlineshop.model.entities.ShoppingCart;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.repository.CartItemRepository;
import org.endava.onlineshop.repository.ProductRepository;
import org.endava.onlineshop.repository.ShoppingCartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

  private final ShoppingCartRepository shoppingCartRepository;
  private final CartItemRepository cartItemRepository;
  private final ProductRepository productRepository;

  public CartService(
      ShoppingCartRepository shoppingCartRepository,
      CartItemRepository cartItemRepository,
      ProductRepository productRepository) {
    this.shoppingCartRepository = shoppingCartRepository;
    this.cartItemRepository = cartItemRepository;
    this.productRepository = productRepository;
  }

  @Transactional(readOnly = true)
  public CartResponseDto getCart(User user, String sessionId) {
    CartOwner owner = resolveOwner(user, sessionId);
    return findCart(owner).map(this::toResponse).orElse(new CartResponseDto(List.of(), 0));
  }

  @Transactional
  public CartResponseDto addItem(User user, String sessionId, AddCartItemRequestDto request) {
    CartOwner owner = resolveOwner(user, sessionId);
    Product product = findActiveProductBySlug(request.productId());
    ShoppingCart cart = findOrCreateCart(owner);

    CartItem item =
        cartItemRepository
            .findByCartIdAndProductId(cart.getId(), product.getId())
            .orElseGet(
                () -> {
                  CartItem newItem = new CartItem();
                  newItem.setCart(cart);
                  newItem.setProduct(product);
                  newItem.setQuantity(0);
                  return newItem;
                });
    item.setQuantity(item.getQuantity() + request.quantity());
    cartItemRepository.save(item);

    return toResponse(cart);
  }

  @Transactional
  public CartResponseDto updateItemQuantity(
      User user, String sessionId, String productId, int quantity) {
    CartOwner owner = resolveOwner(user, sessionId);
    ShoppingCart cart =
        findCart(owner).orElseThrow(() -> new BadRequestException("Cart was not found"));
    Product product = findProductBySlug(productId);

    CartItem item =
        cartItemRepository
            .findByCartIdAndProductId(cart.getId(), product.getId())
            .orElseThrow(() -> new BadRequestException("Product is not in cart"));
    item.setQuantity(quantity);
    cartItemRepository.save(item);

    return toResponse(cart);
  }

  @Transactional
  public CartResponseDto removeItem(User user, String sessionId, String productId) {
    CartOwner owner = resolveOwner(user, sessionId);
    ShoppingCart cart =
        findCart(owner).orElseThrow(() -> new BadRequestException("Cart was not found"));

    CartItem item = null;
    for (CartItem cartItem : cartItemRepository.findAll()) {
      if (cartItem.getCart() != null
          && cartItem.getCart().getId().equals(cart.getId())
          && cartItem.getProduct() != null
          && productId.equals(cartItem.getProduct().getSlug())) {
        item = cartItem;
        break;
      }
    }

    if (item == null) {
      throw new BadRequestException("Product is not in cart");
    }
    cartItemRepository.delete(item);

    return toResponse(cart);
  }

  @Transactional
  public CartResponseDto claimGuestCart(User user, String sessionId) {
    UUID userId = user.getId();
    if (sessionId == null || sessionId.isBlank()) {
      throw new BadRequestException("Guest session id is required");
    }

    ShoppingCart guestCart =
        shoppingCartRepository
            .findBySessionId(sessionId.trim())
            .orElseGet(
                () -> {
                  ShoppingCart userCart = shoppingCartRepository.findByUserId(userId).orElse(null);
                  if (userCart == null) {
                    ShoppingCart emptyCart = new ShoppingCart();
                    emptyCart.setUserId(userId);
                    emptyCart.setSessionId(null);
                    return shoppingCartRepository.save(emptyCart);
                  }
                  return userCart;
                });

    ShoppingCart userCart = shoppingCartRepository.findByUserId(userId).orElse(null);

    if (userCart == null) {
      guestCart.setUserId(userId);
      guestCart.setSessionId(null);
      ShoppingCart savedCart = shoppingCartRepository.save(guestCart);
      return toResponse(savedCart);
    }

    if (userCart.getId().equals(guestCart.getId())) {
      userCart.setSessionId(null);
      ShoppingCart savedCart = shoppingCartRepository.save(userCart);
      return toResponse(savedCart);
    }

    List<CartItem> guestItems =
        cartItemRepository.findByCartIdOrderByCreatedAtAsc(guestCart.getId());
    List<CartItem> userItems = cartItemRepository.findByCartIdOrderByCreatedAtAsc(userCart.getId());
    Map<UUID, CartItem> userItemsByProductId = new HashMap<>();
    for (CartItem userItem : userItems) {
      userItemsByProductId.put(userItem.getProduct().getId(), userItem);
    }

    for (CartItem guestItem : guestItems) {
      CartItem targetItem =
          userItemsByProductId.computeIfAbsent(
              guestItem.getProduct().getId(),
              ignored -> {
                CartItem createdItem = new CartItem();
                createdItem.setCart(userCart);
                createdItem.setProduct(guestItem.getProduct());
                createdItem.setQuantity(0);
                return createdItem;
              });
      targetItem.setQuantity(targetItem.getQuantity() + guestItem.getQuantity());
    }
    cartItemRepository.saveAll(userItemsByProductId.values());

    shoppingCartRepository.delete(guestCart);
    userCart.setSessionId(null);
    ShoppingCart savedCart = shoppingCartRepository.save(userCart);
    return toResponse(savedCart);
  }

  private ShoppingCart findOrCreateCart(CartOwner owner) {
    return findCart(owner)
        .orElseGet(
            () -> {
              ShoppingCart cart = new ShoppingCart();
              cart.setUserId(owner.userId());
              cart.setSessionId(owner.sessionId());
              return shoppingCartRepository.save(cart);
            });
  }

  private java.util.Optional<ShoppingCart> findCart(CartOwner owner) {
    if (owner.userId() != null) {
      return shoppingCartRepository.findByUserId(owner.userId());
    }
    return shoppingCartRepository.findBySessionId(owner.sessionId());
  }

  private Product findActiveProductBySlug(String slug) {
    return productRepository
        .findBySlugAndIsActiveTrue(slug)
        .orElseThrow(() -> new BadRequestException("Product not found"));
  }

  private Product findProductBySlug(String slug) {
    return productRepository
        .findBySlug(slug)
        .orElseThrow(() -> new BadRequestException("Product not found"));
  }

  private CartOwner resolveOwner(User user, String sessionId) {
    if (user != null) {
      return new CartOwner(user.getId(), null);
    }

    if (sessionId == null || sessionId.isBlank()) {
      throw new BadRequestException("Guest session id is required");
    }
    return new CartOwner(null, sessionId.trim());
  }

  private CartResponseDto toResponse(ShoppingCart cart) {
    List<CartItemDto> items =
        cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId()).stream()
            .map(
                item ->
                    new CartItemDto(
                        item.getProduct().getSlug(),
                        item.getProduct().getName(),
                        item.getProduct().getBasePrice(),
                        item.getProduct().getImageId(),
                        item.getQuantity()))
            .toList();
    int totalItems = items.stream().mapToInt(CartItemDto::quantity).sum();
    return new CartResponseDto(items, totalItems);
  }

  private record CartOwner(UUID userId, String sessionId) {}
}
