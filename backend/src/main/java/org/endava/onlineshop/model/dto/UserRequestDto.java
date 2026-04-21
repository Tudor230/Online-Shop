package org.endava.onlineshop.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.endava.onlineshop.model.enums.Role;


public record UserRequestDto(
        @Email(message = "Please enter a valid email format")
        String email,

        @NotNull(message = "Please enter a password")
        @NotBlank
        @Size(min = 6, max = 12, message = "Please enter a password between 6 and 12 characters")
        String password,

        @NotNull(message = "Please enter your first name")
        @NotBlank
        @Size(max = 50, message = "Please enter a name shorter than 50 characters")
        String firstName,

        @NotNull(message = "Please enter your last name")
        @NotBlank
        @Size(max = 50, message = "Please enter a name shorter than 50 characters")
        String lastName,

        @NotNull(message = "Role can not be empty")
        Role role) {
}
