package org.endava.onlineshop.controller;

import java.util.List;
import org.endava.onlineshop.model.dto.order.OrderHistoryEntryDto;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping("/history")
  public List<OrderHistoryEntryDto> getOrderHistory(@AuthenticationPrincipal User user) {
    return orderService.getOrderHistory(user);
  }
}
