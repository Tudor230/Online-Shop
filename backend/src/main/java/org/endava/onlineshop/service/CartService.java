package org.endava.onlineshop.service;

import lombok.RequiredArgsConstructor;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CartService {

    private static final int CURRENCY_SCALE = 2;

    private final ShoppingCartRepository shoppingCartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Value("${stripe.currency:RON}")
    private String stripeCurrency;

    @Value("${stripe.vat-rate:0.19}")
    private BigDecimal vatRate;

    @Value("${stripe.flat-shipping-amount:25.00}")
    private BigDecimal flatShippingAmount;

    @Transactional(readOnly = true)
    public CartResponseDto getCart(User user, String sessionId) {
        CartOwner owner = resolveOwner(user, sessionId);
        return findCart(owner)
                .map(this::toResponse)
                .orElse(emptyCartResponse());
    }

    @Transactional
    public CartResponseDto addItem(User user, String sessionId, AddCartItemRequestDto request) {
        CartOwner owner = resolveOwner(user, sessionId);
        Product product = findActiveProductBySlug(request.productId());
        ShoppingCart cart = findOrCreateCart(owner);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> {
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
    public CartResponseDto updateItemQuantity(User user, String sessionId, String productId, int quantity) {
        CartOwner owner = resolveOwner(user, sessionId);
        ShoppingCart cart = findCart(owner)
                .orElseThrow(() -> new BadRequestException("Cart was not found"));
        Product product = findProductBySlug(productId);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseThrow(() -> new BadRequestException("Product is not in cart"));
        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return toResponse(cart);
    }

    @Transactional
    public CartResponseDto removeItem(User user, String sessionId, String productId) {
        CartOwner owner = resolveOwner(user, sessionId);
        ShoppingCart cart = findCart(owner)
                .orElseThrow(() -> new BadRequestException("Cart was not found"));

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

        ShoppingCart guestCart = shoppingCartRepository.findBySessionId(sessionId.trim())
                .orElseGet(() -> {
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

        List<CartItem> guestItems = cartItemRepository.findByCartIdOrderByCreatedAtAsc(guestCart.getId());
        List<CartItem> userItems = cartItemRepository.findByCartIdOrderByCreatedAtAsc(userCart.getId());
        Map<UUID, CartItem> userItemsByProductId = new HashMap<>();
        for (CartItem userItem : userItems) {
            userItemsByProductId.put(userItem.getProduct().getId(), userItem);
        }

        for (CartItem guestItem : guestItems) {
            CartItem targetItem = userItemsByProductId.computeIfAbsent(guestItem.getProduct().getId(), ignored -> {
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
        return findCart(owner).orElseGet(() -> {
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
        return productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new BadRequestException("Product not found"));
    }

    private Product findProductBySlug(String slug) {
        return productRepository.findBySlug(slug)
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
        List<CartItem> cartItems = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        List<CartItemDto> items = cartItems.stream()
                .map(item -> new CartItemDto(
                        item.getProduct().getSlug(),
                        item.getProduct().getName(),
                        item.getProduct().getBasePrice(),
                        item.getProduct().getImageId(),
                        item.getQuantity()
                ))
                .toList();
        int totalItems = items.stream().mapToInt(CartItemDto::quantity).sum();
        PricingSnapshot pricing = calculatePricing(cartItems);
        return new CartResponseDto(
                items,
                totalItems,
                pricing.subtotal(),
                pricing.shippingAmount(),
                pricing.taxAmount(),
                pricing.totalAmount(),
                normalizedCurrencyCode()
        );
    }

    private CartResponseDto emptyCartResponse() {
        BigDecimal zero = BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        return new CartResponseDto(List.of(), 0, zero, zero, zero, zero, normalizedCurrencyCode());
    }

    private PricingSnapshot calculatePricing(List<CartItem> cartItems) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            BigDecimal unitPrice = cartItem.getProduct().getBasePrice().setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        subtotal = subtotal.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        if (subtotal.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal zero = BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
            return new PricingSnapshot(zero, zero, zero, zero);
        }

        BigDecimal shippingAmount = flatShippingAmount.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(vatRate).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(shippingAmount).add(taxAmount).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        return new PricingSnapshot(subtotal, shippingAmount, taxAmount, totalAmount);
    }

    private String normalizedCurrencyCode() {
        return currencyCodeOrDefault(stripeCurrency);
    }

    private String currencyCodeOrDefault(String currencyCode) {
        return currencyCode == null ? "RON" : currencyCode.trim().toUpperCase();
    }

    private record CartOwner(UUID userId, String sessionId) {
    }

    private record PricingSnapshot(
            BigDecimal subtotal,
            BigDecimal shippingAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount
    ) {
    }
}
