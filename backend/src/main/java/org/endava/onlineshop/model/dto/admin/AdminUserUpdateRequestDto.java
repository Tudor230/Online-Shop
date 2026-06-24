package org.endava.onlineshop.model.dto.admin;

import jakarta.validation.constraints.Size;
import org.endava.onlineshop.model.enums.Role;

public record AdminUserUpdateRequestDto(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        Role role,
        Boolean isActive
) {
}
