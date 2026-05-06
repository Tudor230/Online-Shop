package org.endava.onlineshop.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.checkout.CheckoutStatusResponseDto;
import org.endava.onlineshop.model.dto.checkout.CreateCheckoutSessionRequestDto;
import org.endava.onlineshop.model.dto.checkout.CreateCheckoutSessionResponseDto;
import org.endava.onlineshop.model.entities.Address;
import org.endava.onlineshop.model.entities.CartItem;
import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.entities.OrderItem;
import org.endava.onlineshop.model.entities.OrderStatusHistory;
import org.endava.onlineshop.model.entities.ShoppingCart;
import org.endava.onlineshop.model.entities.StripeWebhookEvent;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.model.enums.OrderStatus;
import org.endava.onlineshop.repository.AddressRepository;
import org.endava.onlineshop.repository.CartItemRepository;
import org.endava.onlineshop.repository.OrderItemRepository;
import org.endava.onlineshop.repository.OrderRepository;
import org.endava.onlineshop.repository.ShoppingCartRepository;
import org.endava.onlineshop.repository.StripeWebhookEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class StripeCheckoutService {

    private static final int CURRENCY_SCALE = 2;

    private final AddressRepository addressRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StripeWebhookEventRepository stripeWebhookEventRepository;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String stripeWebhookSecret;

    @Value("${stripe.success-url}")
    private String stripeSuccessUrl;

    @Value("${stripe.cancel-url}")
    private String stripeCancelUrl;

    @Value("${stripe.currency:RON}")
    private String stripeCurrency;

    @Value("${stripe.vat-rate:0.19}")
    private BigDecimal vatRate;

    @Value("${stripe.flat-shipping-amount:25.00}")
    private BigDecimal flatShippingAmount;


    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public CreateCheckoutSessionResponseDto createCheckoutSession(User user, CreateCheckoutSessionRequestDto request) {
        UUID userId = user.getId();

        Address shippingAddress = addressRepository.findByIdAndUserId(request.shippingAddressId(), userId)
                .orElseThrow(() -> new BadRequestException("Shipping address does not belong to the authenticated user"));
        Address billingAddress = addressRepository.findByIdAndUserId(request.billingAddressId(), userId)
                .orElseThrow(() -> new BadRequestException("Billing address does not belong to the authenticated user"));

        ShoppingCart shoppingCart = shoppingCartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Cart was not found"));

        List<CartItem> cartItems = cartItemRepository.findByCartIdOrderByCreatedAtAsc(shoppingCart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cannot checkout an empty cart");
        }

        PricingSnapshot pricing = calculatePricing(cartItems);

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUserId(userId);
        order.setGuestEmail(null);
        order.setShippingAddressId(shippingAddress.getId());
        order.setBillingAddressId(billingAddress.getId());
        order.setSubtotal(pricing.subtotal());
        order.setDiscountAmount(BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP));
        order.setShippingAmount(pricing.shippingAmount());
        order.setTaxAmount(pricing.taxAmount());
        order.setTotalAmount(pricing.totalAmount());
        order.setCurrencyCode(normalizedCurrencyCode());
        order.setCurrentStatus(OrderStatus.PENDING);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setStatus(OrderStatus.PENDING);
        history.setNotes("Checkout session created");
        order.addStatusHistory(history);

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPriceAtPurchase(cartItem.getProduct().getBasePrice().setScale(CURRENCY_SCALE, RoundingMode.HALF_UP));
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);

        SessionCreateParams params = buildSessionParams(savedOrder, user, shippingAddress, billingAddress, cartItems, pricing);

        try {
            Session session = Session.create(params);
            savedOrder.setStripeCheckoutSessionId(session.getId());
            orderRepository.save(savedOrder);
            clearUserCart(userId);
            return new CreateCheckoutSessionResponseDto(session.getUrl(), savedOrder.getId());
        } catch (StripeException ex) {
            String stripeMessage = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "unknown Stripe error"
                    : ex.getMessage();
            throw new BadRequestException("Failed to create Stripe checkout session: " + stripeMessage);
        }
    }

    @Transactional
    public CreateCheckoutSessionResponseDto createCheckoutSessionForOrder(
            User user,
            UUID orderId,
            CreateCheckoutSessionRequestDto request
    ) {
        UUID userId = user.getId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Order not found"));

        if (!userId.equals(order.getUserId())) {
            throw new BadRequestException("Order does not belong to the authenticated user");
        }

        if (order.getCurrentStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only pending orders can be paid");
        }

        Address shippingAddress = addressRepository.findByIdAndUserId(request.shippingAddressId(), userId)
                .orElseThrow(() -> new BadRequestException("Shipping address does not belong to the authenticated user"));
        Address billingAddress = addressRepository.findByIdAndUserId(request.billingAddressId(), userId)
                .orElseThrow(() -> new BadRequestException("Billing address does not belong to the authenticated user"));

        List<OrderItem> orderItems = order.getItems();
        if (orderItems == null || orderItems.isEmpty()) {
            throw new BadRequestException("Cannot checkout an order with no items");
        }

        order.setShippingAddressId(shippingAddress.getId());
        order.setBillingAddressId(billingAddress.getId());

        SessionCreateParams params = buildSessionParamsForOrder(order, user, shippingAddress, billingAddress, orderItems);

        try {
            Session session = Session.create(params);
            order.setStripeCheckoutSessionId(session.getId());
            orderRepository.save(order);
            return new CreateCheckoutSessionResponseDto(session.getUrl(), order.getId());
        } catch (StripeException ex) {
            String stripeMessage = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "unknown Stripe error"
                    : ex.getMessage();
            throw new BadRequestException("Failed to create Stripe checkout session: " + stripeMessage);
        }
    }

    @Transactional(readOnly = true)
    public CheckoutStatusResponseDto getCheckoutStatus(User user, UUID orderId) {
        UUID userId = user.getId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Order not found"));

        if (!userId.equals(order.getUserId())) {
            throw new BadRequestException("Order does not belong to the authenticated user");
        }

        return new CheckoutStatusResponseDto(order.getId(), order.getCurrentStatus());
    }

    @Transactional
    public void processWebhook(String payload, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new BadRequestException("Missing Stripe signature header");
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException ex) {
            throw new BadRequestException("Invalid Stripe webhook signature");
        }

        if (!recordWebhookEventIfFirstDelivery(event)) {
            return;
        }

        if (!"checkout.session.completed".equals(event.getType())) {
            return;
        }

        Optional<StripeObject> maybeStripeObject = event.getDataObjectDeserializer().getObject();
        if (maybeStripeObject.isEmpty() || !(maybeStripeObject.get() instanceof Session session)) {
            throw new BadRequestException("Invalid Stripe checkout session payload");
        }

        String sessionId = session.getId();
        Order order = orderRepository.findByStripeCheckoutSessionId(sessionId).orElse(null);
        if (order == null) {
            return;
        }

        validateSessionMatchesOrder(session, order);

        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            return;
        }

        if (order.getCurrentStatus() == OrderStatus.PAID) {
            return;
        }

        order.setCurrentStatus(OrderStatus.PAID);
        order.setStripePaymentIntentId(session.getPaymentIntent());
        order.setPaidAt(Instant.now());

        OrderStatusHistory history = new OrderStatusHistory();
        history.setStatus(OrderStatus.PAID);
        history.setNotes("Payment confirmed by Stripe webhook");
        order.addStatusHistory(history);

        orderRepository.save(order);
        
    }

    private PricingSnapshot calculatePricing(List<CartItem> cartItems) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            if (cartItem.getProduct() == null || cartItem.getProduct().getIsActive() == null || !cartItem.getProduct().getIsActive()) {
                throw new BadRequestException("Cart contains inactive products");
            }

            if (cartItem.getQuantity() == null || cartItem.getQuantity() < 1) {
                throw new BadRequestException("Cart contains invalid quantity values");
            }

            if (cartItem.getProduct().getInventory() != null
                    && cartItem.getProduct().getInventory().getQuantityAvailable() != null
                    && cartItem.getQuantity() > cartItem.getProduct().getInventory().getQuantityAvailable()) {
                throw new BadRequestException("Requested quantity is not available for product: " + cartItem.getProduct().getSlug());
            }

            BigDecimal unitPrice = cartItem.getProduct().getBasePrice().setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        subtotal = subtotal.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal shippingAmount = flatShippingAmount.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(vatRate).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(shippingAmount).add(taxAmount).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);

        return new PricingSnapshot(subtotal, shippingAmount, taxAmount, totalAmount);
    }

    private SessionCreateParams buildSessionParams(
            Order order,
            User user,
            Address shippingAddress,
            Address billingAddress,
            List<CartItem> cartItems,
            PricingSnapshot pricing
    ) {
        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeSuccessUrl + "?orderId=" + order.getId())
                .setCancelUrl(stripeCancelUrl + "?orderId=" + order.getId())
                .setCustomerEmail(user.getEmail())
                .putMetadata("orderId", order.getId().toString())
                .putMetadata("userId", user.getId().toString())
                .putMetadata("currency", normalizedCurrencyCode())
                .putMetadata("shippingAddressId", shippingAddress.getId().toString())
                .putMetadata("billingAddressId", billingAddress.getId().toString());
        for (CartItem cartItem : cartItems) {
            builder.addLineItem(buildProductLineItem(cartItem));
        }

        builder.addLineItem(buildChargeLineItem("Shipping", "Flat shipping fee", pricing.shippingAmount(), 1L));
        builder.addLineItem(buildChargeLineItem("Tax", "VAT " + vatRate.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + "%", pricing.taxAmount(), 1L));

        return builder.build();
    }

    private SessionCreateParams buildSessionParamsForOrder(
            Order order,
            User user,
            Address shippingAddress,
            Address billingAddress,
            List<OrderItem> orderItems
    ) {
        String orderCurrencyCode = currencyCodeOrDefault(order.getCurrencyCode());

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeSuccessUrl + "?orderId=" + order.getId())
                .setCancelUrl(stripeCancelUrl + "?orderId=" + order.getId())
                .setCustomerEmail(user.getEmail())
                .putMetadata("orderId", order.getId().toString())
                .putMetadata("userId", user.getId().toString())
                .putMetadata("currency", orderCurrencyCode)
                .putMetadata("shippingAddressId", shippingAddress.getId().toString())
                .putMetadata("billingAddressId", billingAddress.getId().toString());

        for (OrderItem orderItem : orderItems) {
            builder.addLineItem(buildOrderProductLineItem(orderItem, orderCurrencyCode));
        }

        builder.addLineItem(buildChargeLineItem("Shipping", "Flat shipping fee", order.getShippingAmount(), 1L, orderCurrencyCode));
        builder.addLineItem(buildChargeLineItem("Tax", "VAT", order.getTaxAmount(), 1L, orderCurrencyCode));

        return builder.build();
    }

    private SessionCreateParams.LineItem buildProductLineItem(CartItem cartItem) {
        BigDecimal unitPrice = cartItem.getProduct().getBasePrice().setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        return SessionCreateParams.LineItem.builder()
                .setQuantity(Long.valueOf(cartItem.getQuantity()))
                .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(normalizedCurrencyCode().toLowerCase())
                                .setUnitAmount(toMinorUnits(unitPrice))
                                .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName(cartItem.getProduct().getName())
                                                .putMetadata("productSlug", cartItem.getProduct().getSlug())
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    private SessionCreateParams.LineItem buildChargeLineItem(String name, String description, BigDecimal amount, Long quantity) {
        return buildChargeLineItem(name, description, amount, quantity, normalizedCurrencyCode());
    }

    private SessionCreateParams.LineItem buildChargeLineItem(String name, String description, BigDecimal amount, Long quantity, String currencyCode) {
        return SessionCreateParams.LineItem.builder()
                .setQuantity(quantity)
                .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currencyCode.toLowerCase())
                                .setUnitAmount(toMinorUnits(amount))
                                .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName(name)
                                                .setDescription(description)
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    private SessionCreateParams.LineItem buildOrderProductLineItem(OrderItem orderItem, String currencyCode) {
        BigDecimal unitPrice = orderItem.getUnitPriceAtPurchase().setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        return SessionCreateParams.LineItem.builder()
                .setQuantity(Long.valueOf(orderItem.getQuantity()))
                .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currencyCode.toLowerCase())
                                .setUnitAmount(toMinorUnits(unitPrice))
                                .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName(orderItem.getProduct().getName())
                                                .putMetadata("productSlug", orderItem.getProduct().getSlug())
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    private String normalizedCurrencyCode() {
        return currencyCodeOrDefault(stripeCurrency);
    }

    private String currencyCodeOrDefault(String currencyCode) {
        return currencyCode == null ? "RON" : currencyCode.trim().toUpperCase();
    }

    private long toMinorUnits(BigDecimal amount) {
        BigDecimal scaledAmount = amount.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        return scaledAmount.multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    private String generateOrderNumber() {
        return "ORD-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean recordWebhookEventIfFirstDelivery(Event event) {
        StripeWebhookEvent webhookEvent = new StripeWebhookEvent();
        webhookEvent.setStripeEventId(event.getId());
        webhookEvent.setEventType(event.getType());
        try {
            stripeWebhookEventRepository.saveAndFlush(webhookEvent);
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    private void clearUserCart(UUID userId) {
        shoppingCartRepository.findByUserId(userId).ifPresent(cart -> {
            List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
            cartItemRepository.deleteAll(items);
        });
    }

    private void validateSessionMatchesOrder(Session session, Order order) {
        Map<String, String> metadata = session.getMetadata();
        if (metadata == null) {
            throw new BadRequestException("Stripe session metadata is missing");
        }

        String orderIdMetadata = metadata.get("orderId");
        if (!order.getId().toString().equals(orderIdMetadata)) {
            throw new BadRequestException("Stripe session order metadata mismatch");
        }

        String currency = session.getCurrency();
        if (!order.getCurrencyCode().equalsIgnoreCase(currency)) {
            throw new BadRequestException("Stripe session currency mismatch");
        }

        Long amountTotal = session.getAmountTotal();
        if (amountTotal == null) {
            throw new BadRequestException("Stripe session amount is missing");
        }

        long expectedTotal = toMinorUnits(order.getTotalAmount());
        if (amountTotal != expectedTotal) {
            throw new BadRequestException("Stripe session total mismatch");
        }
    }

    private record PricingSnapshot(
            BigDecimal subtotal,
            BigDecimal shippingAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount
    ) {
    }
}
