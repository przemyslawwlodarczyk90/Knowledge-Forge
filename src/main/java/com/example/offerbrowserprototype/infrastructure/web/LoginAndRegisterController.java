package com.example.offerbrowserprototype.infrastructure.web;
import com.example.offerbrowserprototype.domain.dto.confirmationtoken.TokenResponse;
import com.example.offerbrowserprototype.domain.dto.loginandregister.*;
import com.example.offerbrowserprototype.domain.dto.user.UserDTO;
import com.example.offerbrowserprototype.domain.loginaandregister.UserLoginHandler;
import com.example.offerbrowserprototype.infrastructure.facade.LoginAndRegisterFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Operations related to user authentication and profile management")
public class LoginAndRegisterController {

    private final LoginAndRegisterFacade loginAndRegisterFacade;

    public LoginAndRegisterController(LoginAndRegisterFacade loginAndRegisterFacade) {
        this.loginAndRegisterFacade = loginAndRegisterFacade;
    }

    @Operation(summary = "Register a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegistrationResultDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or username taken")
    })
    @PostMapping("/register")
    public ResponseEntity<RegistrationResultDTO> register(
            @Valid @RequestBody RegisterUserDTO registerUserDTO
    ) {
        RegistrationResultDTO result = loginAndRegisterFacade.register(registerUserDTO);
        return result.isSuccess()
                ? new ResponseEntity<>(result, HttpStatus.CREATED)
                : new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }

    @Operation(summary = "User login — returns token, userId, username, email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginDto loginDto) {
        try {
            // ── LoginResult niesie: token, userId, username, email ────────
            UserLoginHandler.LoginResult result = loginAndRegisterFacade.login(loginDto);

            TokenResponse response = new TokenResponse(
                    result.token,
                    result.userId,
                    result.username,
                    result.email,
                    result.role
            );

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                    new TokenResponse("Invalid username or password"),
                    HttpStatus.UNAUTHORIZED
            );
        }
    }

    @Operation(summary = "Update user profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/update")
    public ResponseEntity<UserDTO> updateProfile(
            @Valid @RequestBody UpdateUserDto updateUserDto
    ) {
        try {
            UserDTO updatedUser = loginAndRegisterFacade.updateUserProfile(updateUserDto);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Change user password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Failed to change password")
    })
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordDto changePasswordDto
    ) {
        boolean changed = loginAndRegisterFacade.changeUserPassword(changePasswordDto);
        return changed
                ? new ResponseEntity<>("Password changed successfully", HttpStatus.OK)
                : new ResponseEntity<>("Failed to change password", HttpStatus.BAD_REQUEST);
    }
}