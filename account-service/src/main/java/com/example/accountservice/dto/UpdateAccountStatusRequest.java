package com.example.accountservice.dto;

import com.example.accountservice.model.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull(message = "Account status is required")
        AccountStatus status
) {
}
