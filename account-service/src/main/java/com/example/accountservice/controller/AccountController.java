package com.example.accountservice.controller;

import com.example.accountservice.dto.AccountResponse;
import com.example.accountservice.dto.RegisterAccountRequest;
import com.example.accountservice.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(
            summary = "Register a new account",
            description = "Creates a new passenger or driver account. New registrations start as ACTIVE."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Account created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AccountResponse.class),
                            examples = @ExampleObject(value = "{\"id\":1,\"name\":\"Alice Johnson\",\"email\":\"alice@example.com\",\"role\":\"PASSENGER\",\"status\":\"ACTIVE\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload or validation failure",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Validation failed\",\"errors\":{\"email\":\"Email should be valid\"},\"path\":\"/api/accounts/register\",\"timestamp\":\"2026-09-24T00:00:00\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email is already registered",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":409,\"error\":\"Conflict\",\"message\":\"Email is already registered: alice@example.com\",\"path\":\"/api/accounts/register\",\"timestamp\":\"2026-09-24T00:00:00\"}")
                    )
            )
    })
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RegisterAccountRequest.class),
                    examples = @ExampleObject(value = "{\"name\":\"Alice Johnson\",\"email\":\"alice@example.com\",\"password\":\"Secret123\",\"role\":\"PASSENGER\"}")
            )
    )
    @PostMapping("/register")
    public ResponseEntity<AccountResponse> register(@Valid @org.springframework.web.bind.annotation.RequestBody RegisterAccountRequest request) {
        AccountResponse response = accountService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
