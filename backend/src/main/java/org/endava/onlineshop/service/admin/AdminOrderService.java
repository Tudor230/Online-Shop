package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.model.dto.admin.*;
import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.entities.OrderItem;
import org.endava.onlineshop.model.entities.OrderStatusHistory;
import org.endava.onlineshop.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AdminOrderService {

    private final OrderRepository orderRepository;

    public AdminOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminOrderListDto> getOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toListDto);
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailDto getOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return toDetailDto(order);
    }

    @Transactional
    public AdminOrderDetailDto updateOrderStatus(UUID id, AdminOrderStatusUpdateDto request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(request.newStatus());
        history.setNotes(request.notes());
        order.addStatusHistory(history);

        order.setCurrentStatus(request.newStatus());
        Order saved = orderRepository.save(order);
        return toDetailDto(saved);
    }

    private AdminOrderListDto toListDto(Order order) {
        return new AdminOrderListDto(
                order.getId(), order.getOrderNumber(), order.getUserId(),
                order.getGuestEmail(), order.getCurrentStatus(),
                order.getTotalAmount(), order.getCreatedAt()
        );
    }

    private AdminOrderDetailDto toDetailDto(Order order) {
        return new AdminOrderDetailDto(
                order.getId(), order.getOrderNumber(), order.getUserId(),
                order.getGuestEmail(), order.getCurrentStatus(),
                order.getSubtotal(), order.getDiscountAmount(), order.getTotalAmount(),
                order.getShippingAddressId(), order.getBillingAddressId(),
                order.getItems().stream().map(this::toItemDto).toList(),
                order.getStatusHistory().stream().map(this::toHistoryDto).toList(),
                order.getCreatedAt(), order.getUpdatedAt()
        );
    }

    private AdminOrderItemDto toItemDto(OrderItem item) {
        return new AdminOrderItemDto(
                item.getProduct().getId(), item.getProduct().getName(),
                item.getProduct().getImagePlaceholder(), item.getQuantity(),
                item.getUnitPriceAtPurchase()
        );
    }

    private AdminOrderStatusHistoryDto toHistoryDto(OrderStatusHistory history) {
        return new AdminOrderStatusHistoryDto(
                history.getStatus(), history.getNotes(), history.getCreatedAt()
        );
    }
}
