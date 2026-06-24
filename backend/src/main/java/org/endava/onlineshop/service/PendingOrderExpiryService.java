package org.endava.onlineshop.service;

import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.entities.OrderStatusHistory;
import org.endava.onlineshop.model.enums.OrderStatus;
import org.endava.onlineshop.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PendingOrderExpiryService {

    private static final Duration PENDING_EXPIRY = Duration.ofHours(24);

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    @Scheduled(fixedDelayString = "${orders.pending-expiry.check-delay:PT1H}")
    @Transactional
    public void expirePendingOrders() {
        Instant cutoff = Instant.now().minus(PENDING_EXPIRY);
        List<Order> pendingOrders = orderRepository.findPendingOrdersCreatedBefore(cutoff);
        for (Order order : pendingOrders) {
            if (order.getCurrentStatus() != OrderStatus.PENDING) {
                continue;
            }
            inventoryService.releaseItems(order.getItems());
            order.setCurrentStatus(OrderStatus.CANCELLED);

            OrderStatusHistory history = new OrderStatusHistory();
            history.setStatus(OrderStatus.CANCELLED);
            history.setNotes("Pending order expired after 24h");
            order.addStatusHistory(history);
        }
        orderRepository.saveAll(pendingOrders);
    }
}

