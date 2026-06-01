package com.example.offerbrowserprototype.domain.loginaandregister;

import com.example.offerbrowserprototype.domain.dto.loginandregister.UpdateUserDto;
import com.example.offerbrowserprototype.domain.user.User;
import com.example.offerbrowserprototype.domain.dto.user.UserDTO;
import com.example.offerbrowserprototype.domain.mapper.UserMapper;
import com.example.offerbrowserprototype.infrastructure.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserProfileUpdater {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserProfileUpdater(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public UserDTO updateUserProfile(UpdateUserDto updateUserDto) {

        Long userId = updateUserDto.getId();

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for id: " + userId));

        userMapper.updateUserFromDto(updateUserDto, existingUser);

        User updatedUser = userRepository.save(existingUser);

        return userMapper.toDTO(updatedUser);
    }
}
