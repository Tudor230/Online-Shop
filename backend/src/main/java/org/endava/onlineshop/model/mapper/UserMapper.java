package org.endava.onlineshop.model.mapper;

import org.endava.onlineshop.model.dto.CreateUserRequestDto;
import org.endava.onlineshop.model.dto.UserResponseDto;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.model.enums.Role;

public class UserMapper {

  public User toUserEntity(CreateUserRequestDto dto) {
    User user = new User();

    user.setEmail(dto.email());
    user.setFirstName(dto.firstName());
    user.setLastName(dto.lastName());
    user.setRole(dto.role() != null ? dto.role() : Role.CUSTOMER);
    user.setDefaultShippingAddressId(dto.defaultShippingAddressId());
    user.setDefaultBillingAddressId(dto.defaultBillingAddressId());
    user.setIsActive(dto.isActive() != null ? dto.isActive() : true);

    return user;
  }

  public UserResponseDto toUserResponseDto(User user) {
    return new UserResponseDto(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getRole(),
        user.getDefaultShippingAddressId(),
        user.getDefaultBillingAddressId(),
        user.getIsActive(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
