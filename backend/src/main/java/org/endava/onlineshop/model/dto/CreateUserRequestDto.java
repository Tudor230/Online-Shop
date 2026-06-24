package org.endava.onlineshop.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.endava.onlineshop.model.enums.Role;

import java.util.UUID;

public record CreateUserRequestDto(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must have a valid format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "First name must not be blank")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name must not be blank")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        Role role,
        UUID defaultShippingAddressId,
        UUID defaultBillingAddressId,
        Boolean isActive
) {
}

