package com.example.accountservice.dto;

import com.example.accountservice.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterAccountRequest(
        @NotBlank(message = "Name is required")
        @Schema(description = "Full name of the account holder", example = "Alice Johnson")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        @Schema(description = "Unique account email address", example = "alice@example.com")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        @Schema(description = "Password used to create the account. This value is write-only and never returned in API responses.", example = "Secret123", accessMode = Schema.AccessMode.WRITE_ONLY)
        String password,

        @NotNull(message = "Role is required")
        @Schema(description = "Role for the account", example = "PASSENGER", allowableValues = {"PASSENGER", "DRIVER"})
        Role role
) {
}
