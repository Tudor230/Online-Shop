package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.model.dto.admin.AdminDashboardStatsDto;
import org.endava.onlineshop.model.dto.admin.AdminRevenueChartDto;
import org.endava.onlineshop.model.enums.OrderStatus;
import org.endava.onlineshop.repository.OrderRepository;
import org.endava.onlineshop.repository.ProductInventoryRepository;
import org.endava.onlineshop.repository.ProductRepository;
import org.endava.onlineshop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AdminDashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductInventoryRepository productInventoryRepository;

    public AdminDashboardService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductInventoryRepository productInventoryRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productInventoryRepository = productInventoryRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardStatsDto getStats() {
        long totalOrders = orderRepository.count();
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long lowStockCount = productInventoryRepository.findLowStockItems().size();
        long pendingOrders = orderRepository.findAll().stream()
                .filter(o -> o.getCurrentStatus() == OrderStatus.PENDING)
                .count();
        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AdminDashboardStatsDto(totalOrders, totalUsers, totalProducts, lowStockCount, pendingOrders, totalRevenue);
    }

    @Transactional(readOnly = true)
    public List<AdminRevenueChartDto> getRevenueChart(LocalDate from, LocalDate to) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> !o.getCreatedAt().isBefore(from.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)))
                .filter(o -> !o.getCreatedAt().isAfter(to.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)))
                .collect(java.util.stream.Collectors.groupingBy(
                        o -> o.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                        java.util.stream.Collectors.mapping(
                                o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO,
                                java.util.stream.Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ))
                .entrySet().stream()
                .map(e -> new AdminRevenueChartDto(e.getKey(), e.getValue()))
                .sorted(java.util.Comparator.comparing(AdminRevenueChartDto::date))
                .toList();
    }
}
