package org.endava.onlineshop.controller;

import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.model.dto.order.OrderHistoryEntryDto;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/history")
    public List<OrderHistoryEntryDto> getOrderHistory(@AuthenticationPrincipal User user) {
        return orderService.getOrderHistory(user);
    }

    @PatchMapping("/{orderId}/cancel")
    public OrderHistoryEntryDto cancelOrder(
            @AuthenticationPrincipal User user,
            @PathVariable UUID orderId
    ) {
        return orderService.cancelPendingOrder(user, orderId);
    }
}

