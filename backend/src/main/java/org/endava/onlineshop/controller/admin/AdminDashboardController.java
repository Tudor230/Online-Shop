package org.endava.onlineshop.controller.admin;

import org.endava.onlineshop.model.dto.admin.AdminDashboardStatsDto;
import org.endava.onlineshop.model.dto.admin.AdminRevenueChartDto;
import org.endava.onlineshop.service.admin.AdminDashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/stats")
    public AdminDashboardStatsDto getStats() {
        return adminDashboardService.getStats();
    }

    @GetMapping("/revenue-chart")
    public List<AdminRevenueChartDto> getRevenueChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return adminDashboardService.getRevenueChart(from, to);
    }
}
