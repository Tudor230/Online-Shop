package org.endava.onlineshop.model.mapper;

import org.endava.onlineshop.model.dto.UserRequestDto;
import org.endava.onlineshop.model.dto.UserResponseDto;
import org.endava.onlineshop.model.entities.User;

public class UserMapper {
    public User toUserEntity(UserRequestDto userRequestDto) {
        User user = new User();

        user.setEmail(userRequestDto.email());
        user.setPassword(userRequestDto.password());
        user.setFirstName(userRequestDto.firstName());
        user.setLastName(userRequestDto.lastName());
        user.setRole(userRequestDto.role());

        return user;
    }

    public UserResponseDto toUserResponseDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }
}
