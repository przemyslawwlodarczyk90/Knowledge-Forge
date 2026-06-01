package com.example.offerbrowserprototype.infrastructure.facade;

import com.example.offerbrowserprototype.domain.dto.loginandregister.*;
import com.example.offerbrowserprototype.domain.loginaandregister.*;
import com.example.offerbrowserprototype.infrastructure.repository.UserRepository;
import com.example.offerbrowserprototype.infrastructure.service.ConfirmationTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Testy jednostkowe fasady rejestracji i logowania użytkowników
@ExtendWith(MockitoExtension.class)
class LoginAndRegisterFacadeTest {

    @Mock private UserRegistrationHandler registrationHandler;
    @Mock private UserLoginHandler userLoginHandler;
    @Mock private UserProfileUpdater profileUpdater;
    @Mock private ConfirmationTokenService confirmationTokenService;
    @Mock private UserPasswordChanger passwordChanger;
    @Mock private UserRepository userRepository;
    @InjectMocks private LoginAndRegisterFacade facade;

    @Test
    void register_deleguje_i_zwraca_wynik_rejestracji() {
        // given
        RegisterUserDTO dto = new RegisterUserDTO();
        RegistrationResultDTO expected = new RegistrationResultDTO("jan", true, "Zarejestrowano");
        when(registrationHandler.register(dto)).thenReturn(expected);

        // when
        RegistrationResultDTO result = facade.register(dto);

        // then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void login_deleguje_i_zwraca_login_result_z_tokenem() {
        // given
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername("jan");
        loginDto.setPassword("haslo");
        UserLoginHandler.LoginResult loginResult =
                new UserLoginHandler.LoginResult("tok.en", 1L, "jan", "jan@x.pl", "USER");
        when(userLoginHandler.login(loginDto)).thenReturn(loginResult);

        // when
        UserLoginHandler.LoginResult result = facade.login(loginDto);

        // then — token i dane użytkownika przekazane bez modyfikacji
        assertThat(result.token).isEqualTo("tok.en");
        assertThat(result.userId).isEqualTo(1L);
        assertThat(result.username).isEqualTo("jan");
    }

    @Test
    void changeUserPassword_deleguje_i_zwraca_wynik_bool() {
        // given
        ChangePasswordDto dto = new ChangePasswordDto();
        when(passwordChanger.changeUserPassword(dto)).thenReturn(true);

        // when / then
        assertThat(facade.changeUserPassword(dto)).isTrue();
    }
}
