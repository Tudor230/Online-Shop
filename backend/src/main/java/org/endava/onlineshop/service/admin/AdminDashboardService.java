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
import java.time.Instant;
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
        long lowStockCount = productInventoryRepository.countLowStockItems();
        long pendingOrders = orderRepository.countByCurrentStatus(OrderStatus.PENDING);
        BigDecimal totalRevenue = orderRepository.sumTotalAmount();

        return new AdminDashboardStatsDto(totalOrders, totalUsers, totalProducts, lowStockCount, pendingOrders, totalRevenue);
    }

    @Transactional(readOnly = true)
    public List<AdminRevenueChartDto> getRevenueChart(LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

        return orderRepository.findRevenueBetween(fromInstant, toInstant).stream()
                .map(r -> new AdminRevenueChartDto(
                        (LocalDate) r[0],
                        r[1] != null ? (BigDecimal) r[1] : BigDecimal.ZERO
                ))
                .sorted(java.util.Comparator.comparing(AdminRevenueChartDto::date))
                .toList();
    }
}
