package com.example.knowledgeforge.auth;

import com.example.knowledgeforge.auth.dto.ChangePasswordDto;
import com.example.knowledgeforge.auth.dto.LoginDto;
import com.example.knowledgeforge.auth.dto.RegisterUserDto;
import com.example.knowledgeforge.auth.dto.UpdateUserDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    UserRepository users = mock(UserRepository.class);
    ConfirmationTokenRepository tokens = mock(ConfirmationTokenRepository.class);
    MailService mail = mock(MailService.class);
    JwtService jwt = mock(JwtService.class);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    Clock clock = Clock.fixed(Instant.parse("2030-05-05T10:00:00Z"), ZoneOffset.UTC);
    Properties props = new Properties();
    AuthService service;

    @BeforeEach
    void setUp() {
        props.setProperty("app.frontend.base-url", "http://front");
        service = new AuthService(users, tokens, encoder, jwt, mail, AppConfig.fromProperties(props), clock);
        when(users.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId(7L);
            return u;
        });
    }

    User existing(boolean active) {
        User u = new User();
        u.setId(7L);
        u.setUsername("ala");
        u.setEmail("ala@x.pl");
        u.setPassword(encoder.encode("Secret123"));
        u.setActive(active);
        u.setRole(Role.USER);
        return u;
    }

    @Test
    void register_hashes_password_saves_token_and_sends_activation_link() {
        when(users.findByUsername("ala")).thenReturn(Optional.empty());
        var result = service.register(new RegisterUserDto("ala", "ala@x.pl", "Secret123"));
        assertThat(result.success()).isTrue();
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().isActive()).isFalse();
        assertThat(encoder.matches("Secret123", saved.getValue().getPassword())).isTrue();
        ArgumentCaptor<ConfirmationToken> token = ArgumentCaptor.forClass(ConfirmationToken.class);
        verify(tokens).save(token.capture());
        assertThat(token.getValue().getExpiresAt()).isEqualTo(LocalDateTime.of(2030, 5, 6, 10, 0));
        verify(mail).sendConfirmationEmail(eq("ala@x.pl"), anyString(), eq("ala"), startsWith("http://front/confirm?token="));
    }

    @Test
    void register_rejects_taken_username() {
        when(users.findByUsername("ala")).thenReturn(Optional.of(existing(true)));
        assertThat(service.register(new RegisterUserDto("ala", "a@x.pl", "Secret123")).success()).isFalse();
        verify(users, never()).save(any());
    }

    @Test
    void login_checks_password_and_optionally_activation() {
        when(users.findByUsername("ala")).thenReturn(Optional.of(existing(false)));
        when(jwt.generateToken(any())).thenReturn("tok");
        assertThat(service.login(new LoginDto("ala", "Secret123")).token()).isEqualTo("tok");
        assertThatThrownBy(() -> service.login(new LoginDto("ala", "bad")))
                .isInstanceOf(AuthService.InvalidCredentialsException.class);

        props.setProperty("app.auth.require-email-confirmation", "true");
        assertThatThrownBy(() -> service.login(new LoginDto("ala", "Secret123")))
                .hasMessageContaining("not activated");
    }

    @Test
    void confirm_registration_rules() {
        User u = existing(false);
        when(users.findById(7L)).thenReturn(Optional.of(u));
        ConfirmationToken valid = new ConfirmationToken("t1", LocalDateTime.now(clock), LocalDateTime.now(clock).plusDays(1), 7L);
        when(tokens.findByToken("t1")).thenReturn(Optional.of(valid));
        service.confirmRegistration("t1");
        assertThat(u.isActive()).isTrue();
        assertThat(valid.getConfirmedAt()).isNotNull();

        when(tokens.findByToken("old")).thenReturn(Optional.of(
                new ConfirmationToken("old", LocalDateTime.now(clock).minusDays(3), LocalDateTime.now(clock).minusDays(2), 7L)));
        assertThatThrownBy(() -> service.confirmRegistration("old")).isInstanceOf(ValidationException.class);
        when(tokens.findByToken("none")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirmRegistration("none")).hasMessage("Invalid token");
    }

    @Test
    void profile_and_password_changes() {
        User u = existing(true);
        when(users.findById(7L)).thenReturn(Optional.of(u));
        when(users.findByUsername("zajety")).thenReturn(Optional.of(existing(true)));
        when(users.findByUsername("nowy")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfile(7L, new UpdateUserDto("zajety", null))).isInstanceOf(ConflictException.class);
        assertThat(service.updateProfile(7L, new UpdateUserDto("nowy", "n@x.pl")).username()).isEqualTo("nowy");
        assertThat(service.me(7L).email()).isEqualTo("n@x.pl");
        assertThat(service.changePassword(7L, new ChangePasswordDto("bad", "Other123"))).isFalse();
        assertThat(service.changePassword(7L, new ChangePasswordDto("Secret123", "Other123"))).isTrue();
        assertThat(encoder.matches("Other123", u.getPassword())).isTrue();

        when(users.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.me(99L)).isInstanceOf(NotFoundException.class);
    }
}
