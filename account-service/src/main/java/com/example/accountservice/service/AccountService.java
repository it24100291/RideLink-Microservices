package com.example.accountservice.service;

import com.example.accountservice.dto.AccountResponse;
import com.example.accountservice.dto.RegisterAccountRequest;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.Role;
import com.example.accountservice.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse register(RegisterAccountRequest request) {
        String trimmedName = request.name().trim();
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (request.role() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Public registration as ADMIN is not allowed.");
        }

        if (accountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered: " + normalizedEmail);
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        Account account = new Account(trimmedName, normalizedEmail, hashedPassword, request.role());
        Account saved = accountRepository.save(account);

        return AccountResponse.fromEntity(saved);
    }
}
