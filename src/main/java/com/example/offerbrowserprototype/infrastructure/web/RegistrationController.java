package com.example.offerbrowserprototype.infrastructure.web;

import com.example.offerbrowserprototype.infrastructure.facade.LoginAndRegisterFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller                              // ← nie @RestController — zwracamy widok Thymeleaf
@RequestMapping("/api/v1/registration")
@Tag(name = "Registration", description = "Endpoints for user registration and confirmation")
@Validated
public class RegistrationController {

    private final LoginAndRegisterFacade loginAndRegisterFacade;

    public RegistrationController(LoginAndRegisterFacade loginAndRegisterFacade) {
        this.loginAndRegisterFacade = loginAndRegisterFacade;
    }

    @Operation(summary = "Confirm user registration",
            description = "Confirms the registration of a user by validating the provided confirmation token. " +
                    "If the token is valid, the user account is activated and the confirmation page is shown.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token valid — registration confirmed, confirmation page shown"),
            @ApiResponse(responseCode = "400", description = "Token invalid or expired — error page shown")
    })
    @GetMapping("/confirm")
    public String confirmRegistration(
            @RequestParam("token") @NotBlank String token,
            Model model
    ) {
        try {
            loginAndRegisterFacade.confirmRegistration(token);
            // zwraca templates/registration-confirmed.html
            return "registration-confirmed";
        } catch (Exception e) {
            model.addAttribute("message", e.getMessage());
            // zwraca templates/error.html
            return "error";
        }
    }
}