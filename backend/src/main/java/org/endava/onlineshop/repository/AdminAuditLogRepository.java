package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {

    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AdminAuditLog> findByAdminUserIdOrderByCreatedAtDesc(UUID adminUserId, Pageable pageable);

    Page<AdminAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId, Pageable pageable);

    Page<AdminAuditLog> findByActionTypeOrderByCreatedAtDesc(String actionType, Pageable pageable);

    @Query("SELECT aal FROM AdminAuditLog aal WHERE aal.createdAt >= :from AND aal.createdAt <= :to ORDER BY aal.createdAt DESC")
    Page<AdminAuditLog> findByDateRange(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
