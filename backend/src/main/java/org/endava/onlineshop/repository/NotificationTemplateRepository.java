package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.NotificationTemplate;
import org.endava.onlineshop.model.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    List<NotificationTemplate> findByNotificationType(NotificationType notificationType);
}
