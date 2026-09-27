package com.example.accountservice.controller;

import com.example.accountservice.dto.LoginRequest;
import com.example.accountservice.dto.LoginResponse;
import com.example.accountservice.service.AccountService;
import com.example.accountservice.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accountService;
    private final JwtService jwtService;

    public AuthController(AccountService accountService, JwtService jwtService) {
        this.accountService = accountService;
        this.jwtService = jwtService;
    }

    @Operation(
            summary = "Log in with email and password",
            description = "Verifies credentials and issues a signed access token for a passenger or driver account."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(value = "{\"accessToken\":\"eyJ...\",\"tokenType\":\"Bearer\",\"expiresIn\":3600,\"expiresAt\":\"2026-09-24T12:00:00Z\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid email or password, or suspended account",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid email or password.\",\"path\":\"/api/auth/login\",\"timestamp\":\"2026-09-24T00:00:00\"}")
                    )
            )
    })
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(value = "{\"email\":\"alice@example.com\",\"password\":\"Secret123\"}")
            )
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @org.springframework.web.bind.annotation.RequestBody LoginRequest request) {
        var account = accountService.authenticate(request.email(), request.password());
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(JwtService.TOKEN_TTL_SECONDS);
        String accessToken = jwtService.generateToken(account.getId(), account.getRole(), account.getTokenVersion(), issuedAt, expiresAt);

        return ResponseEntity.ok(new LoginResponse(
                accessToken,
                "Bearer",
                JwtService.TOKEN_TTL_SECONDS,
                expiresAt
        ));
    }
}
