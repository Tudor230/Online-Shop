package org.endava.onlineshop.model.DTO;

import org.endava.onlineshop.model.enums.Role;

public record UserResponseDTO (
        String email,
        String firstName,
        String lastName,
        Role role
){
}
