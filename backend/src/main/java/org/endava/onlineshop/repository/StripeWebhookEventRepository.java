package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.StripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, UUID> {
    boolean existsByStripeEventId(String stripeEventId);
}
