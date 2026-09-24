package com.example.accountservice.controller;

import com.example.accountservice.dto.AccountProfileResponse;
import com.example.accountservice.dto.AccountResponse;
import com.example.accountservice.dto.RegisterAccountRequest;
import com.example.accountservice.dto.UpdateAccountNameRequest;
import com.example.accountservice.dto.UpdateAccountStatusRequest;
import com.example.accountservice.model.Role;
import com.example.accountservice.service.AccountService;
import com.example.accountservice.service.JwtService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final JwtService jwtService;

    public AccountController(AccountService accountService, JwtService jwtService) {
        this.accountService = accountService;
        this.jwtService = jwtService;
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

    @Operation(
            summary = "Get the authenticated account profile",
            description = "Returns the current account profile based on the verified token subject. Authentication is required."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated account profile returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "404", description = "Account not found for the token subject")
    })
    @GetMapping("/me")
    public AccountProfileResponse getCurrentAccount(
            @RequestHeader(value = "Authorization", required = false)
            @Parameter(hidden = true) String authorizationHeader) {
        Long accountId = accountIdFromToken(authorizationHeader);
        accountService.requireActiveAccount(accountId);
        return accountService.getProfileById(accountId);
    }

    @Operation(
            summary = "Update the authenticated account name",
            description = "Updates only the current authenticated account's name. The request body does not accept an account id."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account name updated"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "404", description = "Authenticated account not found")
    })
    @PatchMapping("/me")
    public AccountProfileResponse updateCurrentAccount(
            @RequestHeader(value = "Authorization", required = false)
            @Parameter(hidden = true) String authorizationHeader,
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdateAccountNameRequest request) {
        Long accountId = accountIdFromToken(authorizationHeader);
        accountService.requireActiveAccount(accountId);
        return accountService.updateName(accountId, request.name());
    }

    @Operation(
            summary = "Manage an account status",
            description = "Allows an authenticated ADMIN to set an account to ACTIVE or SUSPENDED. Only verified JWT claims are trusted for the permission check."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "403", description = "Token is valid but the caller does not have ADMIN permission"),
            @ApiResponse(responseCode = "404", description = "Target account not found")
    })
    @PatchMapping("/{id}/status")
    public AccountProfileResponse updateAccountStatus(
            @RequestHeader(value = "Authorization", required = false)
            @Parameter(hidden = true) String authorizationHeader,
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdateAccountStatusRequest request) {
        Claims claims = jwtService.validateBearerToken(authorizationHeader);
        Long currentAccountId = parseSubject(claims);
        var currentAccount = accountService.requireActiveAccount(currentAccountId);
        if (currentAccount.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
        return accountService.changeStatus(id, request.status());
    }

    private Long accountIdFromToken(String authorizationHeader) {
        Claims claims = jwtService.validateBearerToken(authorizationHeader);
        return parseSubject(claims);
    }

    private Long parseSubject(Claims claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token.");
        }
    }
}
