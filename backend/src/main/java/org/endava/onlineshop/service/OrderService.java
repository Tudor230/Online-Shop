package org.endava.onlineshop.service;

import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.order.OrderHistoryEntryDto;
import org.endava.onlineshop.model.dto.order.OrderHistoryItemDto;
import org.endava.onlineshop.model.entities.OrderItem;
import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.repository.OrderRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ObjectProvider<OrderMockSeeder> orderMockSeederProvider;

    public OrderService(
            OrderRepository orderRepository,
            ObjectProvider<OrderMockSeeder> orderMockSeederProvider
    ) {
        this.orderRepository = orderRepository;
        this.orderMockSeederProvider = orderMockSeederProvider;
    }

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
                item.getProduct().getImageId(),
                item.getQuantity(),
                item.getUnitPriceAtPurchase(),
                lineTotal
        );
    }
}

