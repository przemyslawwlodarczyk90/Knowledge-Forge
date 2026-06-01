package com.example.offerbrowserprototype.domain.mapper;

import com.example.offerbrowserprototype.domain.dto.loginandregister.RegisterUserDTO;
import com.example.offerbrowserprototype.domain.dto.loginandregister.UpdateUserDto;
import com.example.offerbrowserprototype.domain.dto.user.UserDTO;
import com.example.offerbrowserprototype.domain.user.Role;
import com.example.offerbrowserprototype.domain.user.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserMapper {


    public UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }


    public User toEntity(RegisterUserDTO registerUserDTO, String hashedPassword) {
        User user = new User();
        user.setUsername(registerUserDTO.getUsername());
        user.setEmail(registerUserDTO.getEmail());
        user.setPassword(hashedPassword);
        user.setRole(Role.USER);
        return user;
    }


    public void updateUserFromDto(UpdateUserDto updateUserDto, User user) {

        if (StringUtils.hasText(updateUserDto.getUsername())) {
            user.setUsername(updateUserDto.getUsername());
        }

        if (StringUtils.hasText(updateUserDto.getEmail())) {
            user.setEmail(updateUserDto.getEmail());
        }

    }
}
