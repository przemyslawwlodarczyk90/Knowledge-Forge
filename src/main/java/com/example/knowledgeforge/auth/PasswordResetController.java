package com.example.knowledgeforge.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService resets;
    private final MailService mail;

    public PasswordResetController(PasswordResetService resets, MailService mail) {
        this.resets = resets;
        this.mail = mail;
    }

    public record ResetRequest(@NotBlank String username) {}
    public record NewPassword(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
                              @NotBlank @Size(min = 6, max = 72) String password) {}

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> request(@Valid @RequestBody ResetRequest request) {
        resets.request(request.username()).ifPresent(email -> mail.sendPasswordResetEmail(email.address(), email.link()));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> reset(@Valid @RequestBody NewPassword request) {
        resets.reset(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }
}
