package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.admin.*;
import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.entities.OrderItem;
import org.endava.onlineshop.model.entities.OrderStatusHistory;
import org.endava.onlineshop.model.enums.OrderStatus;
import org.endava.onlineshop.repository.OrderRepository;
import org.endava.onlineshop.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminOrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, EnumSet.of(OrderStatus.RETURNED),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.RETURNED, EnumSet.noneOf(OrderStatus.class)
    );

    private final OrderRepository orderRepository;
    private final AdminAuditLogService auditLogService;
    private final SecurityUtils securityUtils;

    public AdminOrderService(OrderRepository orderRepository,
                             AdminAuditLogService auditLogService,
                             SecurityUtils securityUtils) {
        this.orderRepository = orderRepository;
        this.auditLogService = auditLogService;
        this.securityUtils = securityUtils;
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

        OrderStatus current = order.getCurrentStatus();
        OrderStatus next = request.newStatus();
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(OrderStatus.class));
        if (!allowed.contains(next)) {
            throw new BadRequestException("Invalid status transition from %s to %s".formatted(current, next));
        }

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(next);
        history.setNotes(request.notes());
        order.addStatusHistory(history);

        order.setCurrentStatus(next);
        Order saved = orderRepository.save(order);
        audit("UPDATE_STATUS", "ORDER", saved.getId().toString(),
                "Changed order %s status from %s to %s".formatted(saved.getOrderNumber(), current, next));
        return toDetailDto(saved);
    }

    private void audit(String action, String entityType, String entityId, String details) {
        auditLogService.log(
                securityUtils.getCurrentUserId().orElse(null),
                securityUtils.getCurrentUserEmail().orElse("system"),
                action, entityType, entityId, details
        );
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
                item.getProduct().getImageId(), item.getQuantity(),
                item.getUnitPriceAtPurchase()
        );
    }

    private AdminOrderStatusHistoryDto toHistoryDto(OrderStatusHistory history) {
        return new AdminOrderStatusHistoryDto(
                history.getStatus(), history.getNotes(), history.getCreatedAt()
        );
    }
}
