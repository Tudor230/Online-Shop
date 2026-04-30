package org.endava.onlineshop.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "stripe_webhook_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StripeWebhookEvent extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "stripe_event_id", nullable = false, unique = true, length = 255)
    private String stripeEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
}
