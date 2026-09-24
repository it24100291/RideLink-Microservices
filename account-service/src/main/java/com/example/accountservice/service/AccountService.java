package com.example.accountservice.service;

import com.example.accountservice.dto.AccountProfileResponse;
import com.example.accountservice.dto.AccountResponse;
import com.example.accountservice.dto.RegisterAccountRequest;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountStatus;
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

        if (trimmedName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required.");
        }

        if (request.role() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Public registration as ADMIN is not allowed.");
        }

        if (accountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered: " + normalizedEmail);
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        Account account = new Account(trimmedName, normalizedEmail, hashedPassword, request.role(), AccountStatus.ACTIVE);
        Account saved = accountRepository.save(account);

        return AccountResponse.fromEntity(saved);
    }

    public AccountResponse verifyCredentials(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isEmpty() || password == null || password.isEmpty()) {
            throwAuthenticationFailure();
        }

        Account account = accountRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(this::authenticationFailure);

        if (account.getStatus() == AccountStatus.SUSPENDED) {
            throwAuthenticationFailure();
        }

        if (!passwordEncoder.matches(password, account.getPassword())) {
            throwAuthenticationFailure();
        }

        return AccountResponse.fromEntity(account);
    }

    public AccountProfileResponse getProfileById(Long accountId) {
        Account account = findAccountOrThrow(accountId);
        return AccountProfileResponse.fromEntity(account);
    }

    public AccountProfileResponse updateName(Long accountId, String newName) {
        String trimmedName = newName == null ? "" : newName.trim();

        if (trimmedName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required.");
        }

        Account account = findAccountOrThrow(accountId);
        account.setName(trimmedName);
        Account saved = accountRepository.save(account);
        return AccountProfileResponse.fromEntity(saved);
    }

    public AccountProfileResponse changeStatus(Long accountId, AccountStatus newStatus) {
        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account status is required.");
        }

        Account account = findAccountOrThrow(accountId);
        account.setStatus(newStatus);
        Account saved = accountRepository.save(account);
        return AccountProfileResponse.fromEntity(saved);
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + accountId));
    }

    private ResponseStatusException authenticationFailure() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }

    private void throwAuthenticationFailure() {
        throw authenticationFailure();
    }
}
