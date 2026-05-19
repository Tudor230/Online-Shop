package org.endava.onlineshop.controller.admin;

import jakarta.validation.Valid;
import org.endava.onlineshop.model.dto.admin.AdminOrderDetailDto;
import org.endava.onlineshop.model.dto.admin.AdminOrderListDto;
import org.endava.onlineshop.model.dto.admin.AdminOrderStatusUpdateDto;
import org.endava.onlineshop.service.admin.AdminOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public Page<AdminOrderListDto> getOrders(Pageable pageable) {
        return adminOrderService.getOrders(pageable);
    }

    @GetMapping("/{id}")
    public AdminOrderDetailDto getOrder(@PathVariable UUID id) {
        return adminOrderService.getOrder(id);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminOrderDetailDto updateOrderStatus(@PathVariable UUID id, @Valid @RequestBody AdminOrderStatusUpdateDto request) {
        return adminOrderService.updateOrderStatus(id, request);
    }
}
