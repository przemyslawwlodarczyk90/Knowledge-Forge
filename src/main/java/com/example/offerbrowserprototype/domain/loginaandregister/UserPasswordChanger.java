package com.example.offerbrowserprototype.domain.loginaandregister;

import com.example.offerbrowserprototype.domain.dto.loginandregister.ChangePasswordDto;
import com.example.offerbrowserprototype.infrastructure.repository.UserRepository;
import com.example.offerbrowserprototype.domain.user.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserPasswordChanger {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserPasswordChanger(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean changeUserPassword(ChangePasswordDto changePasswordDto) {

        return userRepository.findByUsername(changePasswordDto.getUsername())
                .map(user -> {

                    if (passwordEncoder.matches(changePasswordDto.getCurrentPassword(), user.getPassword())) {

                        user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
                        userRepository.save(user);
                        return true;
                    } else {

                        return false;
                    }
                })
                .orElse(false);
    }
}
