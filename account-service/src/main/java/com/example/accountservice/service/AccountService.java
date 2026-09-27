package com.example.accountservice.service;

import com.example.accountservice.dto.AccountProfileResponse;
import com.example.accountservice.dto.AccountResponse;
import com.example.accountservice.dto.RegisterAccountRequest;
import com.example.accountservice.dto.UpdateAccountProfileRequest;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountStatus;
import com.example.accountservice.model.Role;
import com.example.accountservice.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
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
        return AccountResponse.fromEntity(authenticate(email, password));
    }

    public Account authenticate(String email, String password) {
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

        return account;
    }

    public Account getAccountById(Long accountId) {
        return findAccountOrThrow(accountId);
    }

    public Account requireActiveAccount(Long accountId) {
        Account account = findAccountOrThrow(accountId);
        if (account.getStatus() == AccountStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is suspended.");
        }
        return account;
    }

    public AccountProfileResponse getProfileById(Long accountId) {
        Account account = requireActiveAccount(accountId);
        return AccountProfileResponse.fromEntity(account);
    }

    public AccountProfileResponse updateName(Long accountId, String newName) {
        String trimmedName = newName == null ? "" : newName.trim();

        if (trimmedName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required.");
        }

        Account account = requireActiveAccount(accountId);
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

    @Transactional
    public AccountProfileResponse updateProfile(Long accountId, UpdateAccountProfileRequest request) {
        Account account = requireActiveAccount(accountId);
        if (request.name() == null && request.email() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide a name or email to update.");
        }
        String name = request.name() == null ? account.getName() : request.name().trim();
        String email = request.email() == null ? account.getEmail() : request.email().trim().toLowerCase(Locale.ROOT);
        if (name.isBlank() || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and email cannot be blank.");
        }
        if (!email.equals(account.getEmail())) {
            requireCurrentPassword(account, request.currentPassword());
            accountRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
                if (!existing.getId().equals(accountId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
                }
            });
        }
        account.setName(name);
        account.setEmail(email);
        return AccountProfileResponse.fromEntity(accountRepository.saveAndFlush(account));
    }

    @Transactional
    public void changePassword(Long accountId, String currentPassword, String newPassword) {
        Account account = requireActiveAccount(accountId);
        requireCurrentPassword(account, currentPassword);
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 6
                || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters and at most 72 UTF-8 bytes.");
        }
        if (passwordEncoder.matches(newPassword, account.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must differ from the current password.");
        }
        account.setPassword(passwordEncoder.encode(newPassword));
        account.invalidateTokens();
        accountRepository.saveAndFlush(account);
    }

    @Transactional
    public AccountProfileResponse changeRole(Long actorId, Long targetId, Role role) {
        Account actor = requireActiveAccount(actorId);
        if (actor.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required.");
        }
        if (actorId.equals(targetId) && role != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Administrators cannot demote their own account.");
        }
        Account target = findAccountOrThrow(targetId);
        if (target.getRole() != role) {
            target.setRole(role);
            target.invalidateTokens();
        }
        return AccountProfileResponse.fromEntity(accountRepository.saveAndFlush(target));
    }

    private void requireCurrentPassword(Account account, String password) {
        if (password == null || password.isBlank() || !passwordEncoder.matches(password, account.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect or missing.");
        }
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
    }

    private ResponseStatusException authenticationFailure() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }

    private void throwAuthenticationFailure() {
        throw authenticationFailure();
    }
}
