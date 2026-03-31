package org.endava.onlineshop.model.mapper;

import org.endava.onlineshop.model.DTO.UserRequestDTO;
import org.endava.onlineshop.model.entities.User;

public class UserMapper {
    public User toUser(UserRequestDTO userRequestDto){
        User user = new User();

        user.setId(userRequestDto.id());
        user.setEmail(userRequestDto.email());
        user.setPassword(userRequestDto.password());
        user.setFirstName(userRequestDto.firstName());
        user.setLastName(userRequestDto.lastName());
        user.setRole(userRequestDto.role());

        return user;
    }

    public UserRequestDTO toUserRequestDTO(User user){

        return new UserRequestDTO(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }
}
