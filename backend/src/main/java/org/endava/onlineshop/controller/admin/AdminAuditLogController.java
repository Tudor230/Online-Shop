package org.endava.onlineshop.controller.admin;

import org.endava.onlineshop.model.entities.AdminAuditLog;
import org.endava.onlineshop.service.admin.AdminAuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    public AdminAuditLogController(AdminAuditLogService adminAuditLogService) {
        this.adminAuditLogService = adminAuditLogService;
    }

    @GetMapping
    public Page<AdminAuditLog> getAllLogs(Pageable pageable) {
        return adminAuditLogService.getAllLogs(pageable);
    }

    @GetMapping("/admin/{adminUserId}")
    public Page<AdminAuditLog> getLogsByAdmin(@PathVariable UUID adminUserId, Pageable pageable) {
        return adminAuditLogService.getLogsByAdmin(adminUserId, pageable);
    }

    @GetMapping("/entity")
    public Page<AdminAuditLog> getLogsByEntity(
            @RequestParam String entityType,
            @RequestParam String entityId,
            Pageable pageable
    ) {
        return adminAuditLogService.getLogsByEntity(entityType, entityId, pageable);
    }

    @GetMapping("/action-type")
    public Page<AdminAuditLog> getLogsByActionType(
            @RequestParam String actionType,
            Pageable pageable
    ) {
        return adminAuditLogService.getLogsByActionType(actionType, pageable);
    }

    @GetMapping("/date-range")
    public Page<AdminAuditLog> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable
    ) {
        return adminAuditLogService.getLogsByDateRange(from, to, pageable);
    }
}
