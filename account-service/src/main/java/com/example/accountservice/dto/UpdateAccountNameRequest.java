package com.example.accountservice.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAccountNameRequest(
        @NotBlank(message = "Name is required")
        String name
) {
}
