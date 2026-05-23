package org.endava.onlineshop.service.admin;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserSyncStartup {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserSyncStartup.class);

    private final AdminUserService adminUserService;

    @Value("${keycloak.admin.auto-sync.enabled:true}")
    private boolean autoSyncEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        if (!autoSyncEnabled) {
            return;
        }
        try {
            adminUserService.syncUsersFromKeycloak();
            logger.info("Keycloak user sync completed on startup");
        } catch (Exception ex) {
            logger.warn("Keycloak user sync failed on startup", ex);
        }
    }
}

