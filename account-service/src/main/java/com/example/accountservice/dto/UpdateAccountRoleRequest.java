package com.example.accountservice.dto;

import com.example.accountservice.model.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountRoleRequest(@NotNull Role role) { }
