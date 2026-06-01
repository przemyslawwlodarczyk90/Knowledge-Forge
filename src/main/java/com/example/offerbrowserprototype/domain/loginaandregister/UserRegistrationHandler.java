package com.example.offerbrowserprototype.domain.loginaandregister;

import com.example.offerbrowserprototype.domain.dto.loginandregister.RegisterUserDTO;
import com.example.offerbrowserprototype.domain.dto.loginandregister.RegistrationResultDTO;
import com.example.offerbrowserprototype.domain.user.ConfirmationToken;
import com.example.offerbrowserprototype.domain.user.User;
import com.example.offerbrowserprototype.domain.mapper.UserMapper;
import com.example.offerbrowserprototype.infrastructure.repository.UserRepository;
import com.example.offerbrowserprototype.infrastructure.service.ConfirmationTokenService;
import com.example.offerbrowserprototype.infrastructure.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class UserRegistrationHandler {

    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationHandler.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final MailService mailService;
    private final ConfirmationTokenService confirmationTokenService;
    private final Clock clock;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public UserRegistrationHandler(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                   UserMapper userMapper, MailService mailService,
                                   ConfirmationTokenService confirmationTokenService, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.mailService = mailService;
        this.confirmationTokenService = confirmationTokenService;
        this.clock = clock;
    }


    public RegistrationResultDTO register(RegisterUserDTO userDto) {
        logger.info("Starting registration process for user: {}", userDto.getUsername());

        if (userRepository.findByUsername(userDto.getUsername()).isPresent()) {
            logger.warn("Registration failed: Username {} is already taken", userDto.getUsername());
            return new RegistrationResultDTO(null, userDto.getUsername(), false, "Username already taken");
        }

        String hashedPassword = passwordEncoder.encode(userDto.getPassword());
        logger.debug("Password hashed successfully for user: {}", userDto.getUsername());

        User newUser = userMapper.toEntity(userDto, hashedPassword);
        userRepository.save(newUser);
        logger.info("New user saved successfully with ID: {}", newUser.getId());

        String confirmationToken = generateConfirmationToken();
        String confirmationLink = frontendBaseUrl + "/confirm?token=" + confirmationToken;
        logger.debug("Confirmation token generated: {}", confirmationToken);
        logger.debug("Confirmation link: {}", confirmationLink);

        ConfirmationToken token = new ConfirmationToken(
                confirmationToken,
                LocalDateTime.now(clock),
                LocalDateTime.now(clock).plusDays(1),
                newUser.getId()
        );
        confirmationTokenService.saveConfirmationToken(token);
        logger.info("Confirmation token saved successfully for user ID: {}", newUser.getId());

        try {
            mailService.sendConfirmationEmail(userDto.getEmail(), "Confirm your registration", userDto.getUsername(), confirmationLink);
            logger.info("Confirmation email sent successfully to: {}", userDto.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send confirmation email to: {}", userDto.getEmail(), e);
        }

        logger.info("Registration process completed successfully for user: {}", userDto.getUsername());
        return new RegistrationResultDTO(newUser.getId(), userDto.getUsername(), true, "Registration successful");
    }

    private String generateConfirmationToken() {
        String token = java.util.UUID.randomUUID().toString();
        logger.debug("Generated confirmation token: {}", token);
        return token;
    }
}
