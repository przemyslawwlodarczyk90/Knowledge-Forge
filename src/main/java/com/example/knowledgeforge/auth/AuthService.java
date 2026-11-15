package com.example.knowledgeforge.auth;

import com.example.knowledgeforge.auth.dto.ChangePasswordDto;
import com.example.knowledgeforge.auth.dto.LoginDto;
import com.example.knowledgeforge.auth.dto.RegisterUserDto;
import com.example.knowledgeforge.auth.dto.RegistrationResultDto;
import com.example.knowledgeforge.auth.dto.TokenResponse;
import com.example.knowledgeforge.auth.dto.UpdateUserDto;
import com.example.knowledgeforge.auth.dto.UserDto;
import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.domain.exception.ConflictException;
import com.example.knowledgeforge.domain.exception.NotFoundException;
import com.example.knowledgeforge.domain.exception.ValidationException;
import com.example.knowledgeforge.domain.user.ConfirmationToken;
import com.example.knowledgeforge.domain.user.Role;
import com.example.knowledgeforge.domain.user.User;
import com.example.knowledgeforge.repository.ConfirmationTokenRepository;
import com.example.knowledgeforge.repository.UserRepository;
import com.example.knowledgeforge.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Rejestracja, logowanie, aktywacja konta linkiem z maila, zmiana profilu/hasła — przeniesione
 * z mastera (UserRegistrationHandler, UserLoginHandler, ConfirmationTokenService, UserProfileUpdater,
 * UserPasswordChanger) do jednego serwisu.
 */
@Service
public class AuthService {

    private static final Logger log = Logger.getLogger(AuthService.class.getName());

    /** Komunikat celowo ten sam dla złej nazwy i złego hasła — nie zdradza, które konta istnieją. */
    public static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserRepository userRepository;
    private final ConfirmationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final AppConfig config;
    private final Clock clock;

    public AuthService(UserRepository userRepository, ConfirmationTokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, MailService mailService,
                       AppConfig config, Clock clock) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.config = config;
        this.clock = clock;
    }

    /** Wynik nieudanej próby logowania. */
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }

    @Transactional
    public RegistrationResultDto register(RegisterUserDto dto) {
        log.info(() -> "Starting registration process for user: " + dto.username());

        if (userRepository.findByUsername(dto.username()).isPresent()) {
            log.warning(() -> "Registration failed: Username " + dto.username() + " is already taken");
            return new RegistrationResultDto(null, dto.username(), false, "Username already taken");
        }

        User user = new User();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(Role.USER);
        user.setActive(false);
        userRepository.save(user);
        log.info(() -> "New user saved successfully with ID: " + user.getId());

        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(clock);
        tokenRepository.save(new ConfirmationToken(token, now, now.plusDays(1), user.getId()));

        String confirmationLink = config.frontendBaseUrl() + "/confirm?token=" + token;
        mailService.sendConfirmationEmail(dto.email(), "Confirm your registration", dto.username(), confirmationLink);

        return new RegistrationResultDto(user.getId(), dto.username(), true, "Registration successful");
    }

    public TokenResponse login(LoginDto dto) {
        User user = userRepository.findByUsername(dto.username())
                .filter(u -> u.getPassword() != null && passwordEncoder.matches(dto.password(), u.getPassword()))
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));
        if (config.requireEmailConfirmation() && !user.isActive()) {
            throw new InvalidCredentialsException("Account is not activated — check your email for the confirmation link");
        }
        String token = jwtService.generateToken(user);
        return new TokenResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public String confirmRegistration(String token) {
        ConfirmationToken ct = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException("Invalid token"));
        if (ct.getConfirmedAt() == null) {
            if (ct.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
                throw new ValidationException("Token has expired");
            }
            ct.setConfirmedAt(LocalDateTime.now(clock));
        }
        User user = userRepository.findById(ct.getUserId())
                .orElseThrow(() -> new ValidationException("User not found"));
        user.setActive(true);
        return "Registration confirmed!";
    }

    public UserDto me(Long userId) {
        return UserDto.from(userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found")));
    }

    @Transactional
    public UserDto updateProfile(Long userId, UpdateUserDto dto) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (dto.username() != null && !dto.username().isBlank() && !dto.username().equals(user.getUsername())) {
            if (userRepository.findByUsername(dto.username()).isPresent()) {
                throw new ConflictException("Username already taken");
            }
            user.setUsername(dto.username());
        }
        if (dto.email() != null && !dto.email().isBlank()) {
            user.setEmail(dto.email());
        }
        return UserDto.from(user);
    }

    @Transactional
    public boolean changePassword(Long userId, ChangePasswordDto dto) {
        return userRepository.findById(userId)
                .filter(u -> u.getPassword() != null && passwordEncoder.matches(dto.currentPassword(), u.getPassword()))
                .map(u -> {
                    u.setPassword(passwordEncoder.encode(dto.newPassword()));
                    return true;
                })
                .orElse(false);
    }
}
