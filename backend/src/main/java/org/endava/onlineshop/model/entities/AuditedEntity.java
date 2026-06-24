package org.endava.onlineshop.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@MappedSuperclass
@Getter
public abstract class AuditedEntity extends CreationAuditedEntity {

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}

