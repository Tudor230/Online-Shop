package org.endava.onlineshop.service;

import lombok.RequiredArgsConstructor;
import org.endava.onlineshop.model.dto.UserRequestDto;
import org.endava.onlineshop.model.dto.UserResponseDto;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.model.mapper.UserMapper;
import org.endava.onlineshop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper = new UserMapper();

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponseDto)
                .toList();
    }

    public UserResponseDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toUserResponseDto)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + id));
    }

    @Transactional
    public UserResponseDto createUser(UserRequestDto request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email is already in use");
        }
        User user = userMapper.toUserEntity(request);
        return userMapper.toUserResponseDto(userRepository.save(user));
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UserRequestDto request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + id));

        existingUser.setEmail(request.email());
        existingUser.setFirstName(request.firstName());
        existingUser.setLastName(request.lastName());
        existingUser.setRole(request.role());
        existingUser.setPassword(request.password());

        return userMapper.toUserResponseDto(userRepository.save(existingUser));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NoSuchElementException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}