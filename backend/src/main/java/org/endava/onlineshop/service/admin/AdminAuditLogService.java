package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.model.entities.AdminAuditLog;
import org.endava.onlineshop.repository.AdminAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminAuditLogService {

    private final AdminAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AdminAuditLogService(AdminAuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void log(UUID adminUserId, String adminEmail, String actionType, String entityType, String entityId, String details) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUserId(adminUserId);
        log.setAdminEmail(adminEmail);
        log.setActionType(actionType);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        
        String jsonDetails;
        try {
            jsonDetails = objectMapper.writeValueAsString(Map.of("message", details));
        } catch (Exception e) {
            jsonDetails = "{}";
        }
        
        log.setDetails(jsonDetails);
        log.setCreatedAt(Instant.now());
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> getLogsByAdmin(UUID adminUserId, Pageable pageable) {
        return auditLogRepository.findByAdminUserIdOrderByCreatedAtDesc(adminUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> getLogsByEntity(String entityType, String entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> getLogsByActionType(String actionType, Pageable pageable) {
        return auditLogRepository.findByActionTypeOrderByCreatedAtDesc(actionType, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> getLogsByDateRange(Instant from, Instant to, Pageable pageable) {
        return auditLogRepository.findByDateRange(from, to, pageable);
    }
}
