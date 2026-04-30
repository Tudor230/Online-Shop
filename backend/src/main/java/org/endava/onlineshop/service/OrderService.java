package org.endava.onlineshop.service;

import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.order.OrderHistoryEntryDto;
import org.endava.onlineshop.model.dto.order.OrderHistoryItemDto;
import org.endava.onlineshop.model.entities.OrderItem;
import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.entities.OrderStatusHistory;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.model.enums.OrderStatus;
import org.endava.onlineshop.repository.OrderRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ObjectProvider<OrderMockSeeder> orderMockSeederProvider;


    @Transactional
    public List<OrderHistoryEntryDto> getOrderHistory(User user) {
        OrderMockSeeder orderMockSeeder = orderMockSeederProvider.getIfAvailable();
        if (orderMockSeeder != null) {
            orderMockSeeder.seedForUserIfMissing(user);
        }

        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toOrderHistoryDto)
                .toList();
    }

    @Transactional
    public OrderHistoryEntryDto cancelPendingOrder(User user, UUID orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Order not found"));

        if (!user.getId().equals(order.getUserId())) {
            throw new BadRequestException("Order does not belong to the authenticated user");
        }

        if (order.getCurrentStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only pending orders can be cancelled");
        }

        order.setCurrentStatus(OrderStatus.CANCELLED);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setStatus(OrderStatus.CANCELLED);
        history.setNotes("Cancelled by user from order history");
        order.addStatusHistory(history);

        Order savedOrder = orderRepository.save(order);
        return toOrderHistoryDto(savedOrder);
    }

    private OrderHistoryEntryDto toOrderHistoryDto(Order order) {
        return new OrderHistoryEntryDto(
                order.getId().toString(),
                order.getOrderNumber(),
                order.getCurrentStatus(),
                order.getCreatedAt(),
                order.getSubtotal(),
                order.getShippingAmount(),
                order.getTaxAmount(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getCurrencyCode(),
                order.getItems().stream().map(this::toOrderHistoryItemDto).toList()
        );
    }

    private OrderHistoryItemDto toOrderHistoryItemDto(OrderItem item) {
        BigDecimal lineTotal = item.getUnitPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderHistoryItemDto(
                item.getProduct().getSlug(),
                item.getProduct().getName(),
                item.getProduct().getImageId(),
                item.getQuantity(),
                item.getUnitPriceAtPurchase(),
                lineTotal
        );
    }
}

