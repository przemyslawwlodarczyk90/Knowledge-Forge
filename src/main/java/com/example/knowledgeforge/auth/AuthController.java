package com.example.knowledgeforge.auth;

import com.example.knowledgeforge.auth.dto.ChangePasswordDto;
import com.example.knowledgeforge.auth.dto.LoginDto;
import com.example.knowledgeforge.auth.dto.RegisterUserDto;
import com.example.knowledgeforge.auth.dto.RegistrationResultDto;
import com.example.knowledgeforge.auth.dto.TokenResponse;
import com.example.knowledgeforge.auth.dto.UpdateUserDto;
import com.example.knowledgeforge.auth.dto.UserDto;
import com.example.knowledgeforge.security.AuthCookies;
import com.example.knowledgeforge.security.JwtService;
import com.example.knowledgeforge.service.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * /api/auth — rejestracja, logowanie (JWT w body + ciasteczko KF_TOKEN), wylogowanie, profil.
 * /api/v1/registration/confirm — aktywacja konta tokenem z maila (ścieżka jak na masterze; frontend
 * /confirm?token=... woła ją przez axios, więc odpowiada JSON-em zamiast strony Thymeleaf).
 */
@RestController
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, JwtService jwtService, CurrentUser currentUser) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<RegistrationResultDto> register(@Valid @RequestBody RegisterUserDto dto) {
        RegistrationResultDto result = authService.register(dto);
        return ResponseEntity.status(result.success() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST).body(result);
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto dto, HttpServletRequest req) {
        try {
            TokenResponse response = authService.login(dto);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE,
                            AuthCookies.issue(response.token(), jwtService.expirationTimeMs(), req.isSecure()).toString())
                    .body(response);
        } catch (AuthService.InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req) {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, AuthCookies.clear(req.isSecure()).toString())
                .build();
    }

    @GetMapping("/api/auth/me")
    public UserDto me() {
        return authService.me(currentUser.id());
    }

    @PutMapping("/api/auth/update")
    public UserDto updateProfile(@Valid @RequestBody UpdateUserDto dto) {
        return authService.updateProfile(currentUser.id(), dto);
    }

    @PostMapping("/api/auth/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordDto dto) {
        boolean changed = authService.changePassword(currentUser.id(), dto);
        return changed
                ? ResponseEntity.ok(Map.of("message", "Password changed successfully"))
                : ResponseEntity.badRequest().body(Map.of("message", "Failed to change password"));
    }

    @GetMapping("/api/v1/registration/confirm")
    public Map<String, String> confirm(@RequestParam("token") String token) {
        return Map.of("message", authService.confirmRegistration(token));
    }
}
