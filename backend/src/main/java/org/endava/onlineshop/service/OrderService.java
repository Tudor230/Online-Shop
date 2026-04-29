package org.endava.onlineshop.service;

import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.order.OrderHistoryEntryDto;
import org.endava.onlineshop.model.dto.order.OrderHistoryItemDto;
import org.endava.onlineshop.model.entities.OrderItem;
import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.repository.OrderRepository;
import org.endava.onlineshop.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderMockSeeder orderMockSeeder;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, OrderMockSeeder orderMockSeeder) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderMockSeeder = orderMockSeeder;
    }

    @Transactional
    public List<OrderHistoryEntryDto> getOrderHistory(Jwt jwt) {
        User user = getCurrentUser(jwt);
        orderMockSeeder.seedForUserIfMissing(user);

        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toOrderHistoryDto)
                .toList();
    }

    private User getCurrentUser(Jwt jwt) {
        UUID userId = parseUserId(jwt.getSubject());
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Authenticated user not found"));
    }

    private UUID parseUserId(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new BadRequestException("Invalid authentication subject");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid authentication subject");
        }
    }

    private OrderHistoryEntryDto toOrderHistoryDto(Order order) {
        return new OrderHistoryEntryDto(
                order.getId().toString(),
                order.getOrderNumber(),
                order.getCurrentStatus(),
                order.getCreatedAt(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getItems().stream().map(this::toOrderHistoryItemDto).toList()
        );
    }

    private OrderHistoryItemDto toOrderHistoryItemDto(OrderItem item) {
        BigDecimal lineTotal = item.getUnitPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderHistoryItemDto(
                item.getProduct().getSlug(),
                item.getProduct().getName(),
                item.getProduct().getImagePlaceholder(),
                item.getQuantity(),
                item.getUnitPriceAtPurchase(),
                lineTotal
        );
    }
}

