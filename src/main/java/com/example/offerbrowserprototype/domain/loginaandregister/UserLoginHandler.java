package com.example.offerbrowserprototype.domain.loginaandregister;

import com.example.offerbrowserprototype.domain.dto.loginandregister.LoginDto;
import com.example.offerbrowserprototype.infrastructure.repository.UserRepository;
import com.example.offerbrowserprototype.infrastructure.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Component
public class UserLoginHandler {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService      jwtService;

    public UserLoginHandler(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService      = jwtService;
    }

    public static class LoginResult {
        public final String token;
        public final Long   userId;
        public final String username;
        public final String email;
        public final String role;

        public LoginResult(String token, Long userId, String username, String email, String role) {
            this.token    = token;
            this.userId   = userId;
            this.username = username;
            this.email    = email;
            this.role     = role;
        }
    }

    public LoginResult login(LoginDto loginDto) {
        return userRepository.findByUsername(loginDto.getUsername())
                .filter(user -> passwordEncoder.matches(loginDto.getPassword(), user.getPassword()))
                .map(user -> {
                    String roleName = user.getRole() != null ? user.getRole().name() : "USER";

                    org.springframework.security.core.userdetails.User principal =
                            new org.springframework.security.core.userdetails.User(
                                    user.getUsername(),
                                    user.getPassword(),
                                    List.of(new SimpleGrantedAuthority("ROLE_" + roleName))
                            );

                    String token = jwtService.generateToken(principal);

                    return new LoginResult(token, user.getId(), user.getUsername(), user.getEmail(), roleName);
                })
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
    }
}