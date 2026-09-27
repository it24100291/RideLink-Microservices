package com.example.accountservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAccountProfileRequest(
        @Size(max = 255) String name,
        @Email @Size(max = 255) String email,
        @io.swagger.v3.oas.annotations.media.Schema(accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY)
        String currentPassword
) {
    public UpdateAccountProfileRequest {
        if (email != null) {
            email = email.trim();
        }
    }

    @Override
    public String toString() {
        return "UpdateAccountProfileRequest[credentials redacted]";
    }
}
