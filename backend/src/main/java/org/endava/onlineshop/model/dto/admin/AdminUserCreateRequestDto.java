package org.endava.onlineshop.model.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.endava.onlineshop.model.enums.Role;

public record AdminUserCreateRequestDto(
        @NotNull @Email @Size(max = 255) String email,
        @NotNull @Size(max = 100) String firstName,
        @NotNull @Size(max = 100) String lastName,
        @NotNull Role role,
        @NotNull @Size(min = 8, max = 100) String password
) {
}
