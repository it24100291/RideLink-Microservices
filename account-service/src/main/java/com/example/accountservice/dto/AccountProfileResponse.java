package com.example.accountservice.dto;

import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountStatus;
import com.example.accountservice.model.Role;

public record AccountProfileResponse(Long id, String name, String email, Role role, AccountStatus status) {
    public static AccountProfileResponse fromEntity(Account account) {
        return new AccountProfileResponse(
                account.getId(),
                account.getName(),
                account.getEmail(),
                account.getRole(),
                account.getStatus()
        );
    }
}
