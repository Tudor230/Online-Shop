package org.endava.onlineshop.service;

import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.order.OrderHistoryEntryDto;
import org.endava.onlineshop.model.dto.order.OrderHistoryItemDto;
import org.endava.onlineshop.model.entities.OrderItem;
import org.endava.onlineshop.model.entities.Order;
import org.endava.onlineshop.model.entities.OrderStatusHistory;
import org.endava.onlineshop.model.entities.Review;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.model.enums.OrderStatus;
import org.endava.onlineshop.repository.OrderRepository;
import org.endava.onlineshop.repository.ReviewRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OrderService {

    private static final EnumSet<OrderStatus> REVIEW_ELIGIBLE_ORDER_STATUSES = EnumSet.of(
            OrderStatus.PAID,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED,
            OrderStatus.RETURNED
    );

    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final InventoryService inventoryService;
    private final ObjectProvider<OrderMockSeeder> orderMockSeederProvider;


    @Transactional
    public List<OrderHistoryEntryDto> getOrderHistory(User user) {
        OrderMockSeeder orderMockSeeder = orderMockSeederProvider.getIfAvailable();
        if (orderMockSeeder != null) {
            orderMockSeeder.seedForUserIfMissing(user);
        }

        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(order -> toOrderHistoryDto(order, user.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderHistoryEntryDto getOrderDetails(User user, String orderSlug) {
        OrderMockSeeder orderMockSeeder = orderMockSeederProvider.getIfAvailable();
        if (orderMockSeeder != null) {
            orderMockSeeder.seedForUserIfMissing(user);
        }

        Order order = orderRepository.findByOrderNumberAndUserId(orderSlug, user.getId())
                .orElseThrow(() -> new BadRequestException("Order was not found"));
        return toOrderHistoryDto(order, user.getId());
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

        inventoryService.releaseItems(order.getItems());
        order.setCurrentStatus(OrderStatus.CANCELLED);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setStatus(OrderStatus.CANCELLED);
        history.setNotes("Cancelled by user from order history");
        order.addStatusHistory(history);

        Order savedOrder = orderRepository.save(order);
        return toOrderHistoryDto(savedOrder, user.getId());
    }

    private OrderHistoryEntryDto toOrderHistoryDto(Order order, UUID userId) {
        List<UUID> productIds = order.getItems().stream().map(item -> item.getProduct().getId()).distinct().toList();
        Map<UUID, ProductReviewSnapshot> reviewSnapshotByProductId = productIds.isEmpty()
                ? Map.of()
                : reviewRepository.summarizeByProductIds(productIds).stream()
                        .collect(Collectors.toMap(
                                ReviewRepository.ProductReviewAggregate::getProductId,
                                aggregate -> new ProductReviewSnapshot(
                                        aggregate.getAverageRating() != null ? aggregate.getAverageRating() : 0.0d,
                                        aggregate.getReviewCount() != null ? aggregate.getReviewCount().intValue() : 0
                                ),
                                (left, right) -> left
                        ));

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
                order.getItems().stream()
                        .map(item -> toOrderHistoryItemDto(item, userId, order.getCurrentStatus(), reviewSnapshotByProductId))
                        .toList()
        );
    }

    private OrderHistoryItemDto toOrderHistoryItemDto(
            OrderItem item,
            UUID userId,
            OrderStatus orderStatus,
            Map<UUID, ProductReviewSnapshot> reviewSnapshotByProductId
    ) {
        BigDecimal lineTotal = item.getUnitPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()));
        String category = item.getProduct().getCategories().stream()
                .map(categoryItem -> categoryItem.getName())
                .sorted()
                .findFirst()
                .orElse("Uncategorized");

        ProductReviewSnapshot reviewSnapshot = reviewSnapshotByProductId.getOrDefault(
                item.getProduct().getId(),
                ProductReviewSnapshot.empty()
        );
        Review review = reviewRepository.findByProductIdAndUserId(item.getProduct().getId(), userId).orElse(null);
        boolean reviewed = review != null;
        boolean canLeaveReview = !reviewed && REVIEW_ELIGIBLE_ORDER_STATUSES.contains(orderStatus);

        return new OrderHistoryItemDto(
                item.getProduct().getSlug(),
                category,
                item.getProduct().getName(),
                item.getProduct().getImageId(),
                item.getProduct().getDescription(),
                reviewSnapshot.averageRating(),
                reviewSnapshot.reviewCount(),
                item.getQuantity(),
                item.getUnitPriceAtPurchase(),
                lineTotal,
                canLeaveReview,
                reviewed,
                review != null ? review.getId().toString() : null,
                review != null ? review.getRating().intValue() : null
        );
    }

    private record ProductReviewSnapshot(double averageRating, int reviewCount) {
        private static ProductReviewSnapshot empty() {
            return new ProductReviewSnapshot(0.0d, 0);
        }
    }
}

