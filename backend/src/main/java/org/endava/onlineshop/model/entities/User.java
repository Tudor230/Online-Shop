package org.endava.onlineshop.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.endava.onlineshop.model.enums.Role;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "`user`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseAuditEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "role", nullable = false, columnDefinition = "user_role")
    private Role role = Role.CUSTOMER;

    @Column(name = "default_shipping_address_id")
    private UUID defaultShippingAddressId;

    @Column(name = "default_billing_address_id")
    private UUID defaultBillingAddressId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}

