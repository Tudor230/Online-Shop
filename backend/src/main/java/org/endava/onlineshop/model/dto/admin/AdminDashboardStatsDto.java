package org.endava.onlineshop.model.dto.admin;

import java.math.BigDecimal;

public record AdminDashboardStatsDto(
        Long totalOrders,
        Long totalUsers,
        Long totalProducts,
        Long lowStockCount,
        Long pendingOrders,
        BigDecimal totalRevenue
) {
}
