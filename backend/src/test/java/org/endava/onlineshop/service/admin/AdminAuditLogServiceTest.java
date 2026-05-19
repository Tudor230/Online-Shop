package org.endava.onlineshop.service.admin;

import org.endava.onlineshop.model.entities.AdminAuditLog;
import org.endava.onlineshop.repository.AdminAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceTest {

    @Mock
    private AdminAuditLogRepository auditLogRepository;

    @Spy
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @InjectMocks
    private AdminAuditLogService auditLogService;

    private UUID adminId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        pageable = PageRequest.of(0, 20);
    }

    @Test
    void shouldLogAuditEntry() {
        UUID entityId = UUID.randomUUID();
        auditLogService.log(adminId, "admin@test.com", "CREATE", "PRODUCT", entityId.toString(), "Created product X");

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getAdminUserId()).isEqualTo(adminId);
        assertThat(saved.getAdminEmail()).isEqualTo("admin@test.com");
        assertThat(saved.getActionType()).isEqualTo("CREATE");
        assertThat(saved.getEntityType()).isEqualTo("PRODUCT");
        assertThat(saved.getEntityId()).isEqualTo(entityId.toString());
        assertThat(saved.getDetails()).isEqualTo("{\"message\":\"Created product X\"}");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldHandleNullAdminUserId() {
        auditLogService.log(null, "system", "DELETE", "USER", "123", "Deleted user");

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getAdminUserId()).isNull();
    }

    @Test
    void shouldReturnAllLogs() {
        AdminAuditLog log = new AdminAuditLog();
        log.setId(UUID.randomUUID());
        Page<AdminAuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

        Page<AdminAuditLog> result = auditLogService.getAllLogs(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findAllByOrderByCreatedAtDesc(pageable);
    }

    @Test
    void shouldReturnLogsByAdmin() {
        AdminAuditLog log = new AdminAuditLog();
        Page<AdminAuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findByAdminUserIdOrderByCreatedAtDesc(adminId, pageable)).thenReturn(page);

        Page<AdminAuditLog> result = auditLogService.getLogsByAdmin(adminId, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByAdminUserIdOrderByCreatedAtDesc(adminId, pageable);
    }

    @Test
    void shouldReturnLogsByEntity() {
        AdminAuditLog log = new AdminAuditLog();
        Page<AdminAuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("PRODUCT", "123", pageable)).thenReturn(page);

        Page<AdminAuditLog> result = auditLogService.getLogsByEntity("PRODUCT", "123", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByEntityTypeAndEntityIdOrderByCreatedAtDesc("PRODUCT", "123", pageable);
    }

    @Test
    void shouldReturnLogsByActionType() {
        AdminAuditLog log = new AdminAuditLog();
        Page<AdminAuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findByActionTypeOrderByCreatedAtDesc("CREATE", pageable)).thenReturn(page);

        Page<AdminAuditLog> result = auditLogService.getLogsByActionType("CREATE", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByActionTypeOrderByCreatedAtDesc("CREATE", pageable);
    }

    @Test
    void shouldReturnLogsByDateRange() {
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        AdminAuditLog log = new AdminAuditLog();
        Page<AdminAuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findByDateRange(from, to, pageable)).thenReturn(page);

        Page<AdminAuditLog> result = auditLogService.getLogsByDateRange(from, to, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByDateRange(from, to, pageable);
    }
}
