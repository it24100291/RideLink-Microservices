package com.example.accountservice;

import com.example.accountservice.dto.AccountProfileResponse;
import com.example.accountservice.dto.AccountResponse;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountStatus;
import com.example.accountservice.model.Role;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AccountServiceProfileTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void getProfileById_returnsSafeProfile() {
        Account account = accountRepository.save(new Account(
                "Alice Johnson",
                "alice@example.com",
                "encoded-password",
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));

        AccountProfileResponse profile = accountService.getProfileById(account.getId());

        assertThat(profile.id()).isEqualTo(account.getId());
        assertThat(profile.name()).isEqualTo("Alice Johnson");
        assertThat(profile.email()).isEqualTo("alice@example.com");
        assertThat(profile.role()).isEqualTo(Role.PASSENGER);
        assertThat(profile.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(profile).hasNoNullFieldsOrProperties();
    }

    @Test
    void getProfileById_missingAccount_throwsNotFound() {
        assertThatThrownBy(() -> accountService.getProfileById(999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException exception = (ResponseStatusException) ex;
                    assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                });
    }

    @Test
    void updateName_validName_updatesProfile() {
        Account account = accountRepository.save(new Account(
                "Old Name",
                "old@example.com",
                "encoded-password",
                Role.DRIVER,
                AccountStatus.ACTIVE
        ));

        AccountProfileResponse updated = accountService.updateName(account.getId(), "  New Name  ");

        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(accountRepository.findById(account.getId()).orElseThrow().getName()).isEqualTo("New Name");
    }

    @Test
    void updateName_invalidName_throwsBadRequest() {
        Account account = accountRepository.save(new Account(
                "Alice",
                "alice2@example.com",
                "encoded-password",
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));

        assertThatThrownBy(() -> accountService.updateName(account.getId(), "   "))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException exception = (ResponseStatusException) ex;
                    assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                });
    }

    @Test
    void changeStatus_updatesStatus() {
        Account account = accountRepository.save(new Account(
                "Alice",
                "alice3@example.com",
                "encoded-password",
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));

        AccountProfileResponse updated = accountService.changeStatus(account.getId(), AccountStatus.SUSPENDED);

        assertThat(updated.status()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(accountRepository.findById(account.getId()).orElseThrow().getStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    void verifyCredentials_validCredentials_returnsAccountSummary() {
        String rawPassword = "StrongPass123";
        Account account = accountRepository.save(new Account(
                "Alice Johnson",
                "alice@example.com",
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(rawPassword),
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));

        AccountResponse response = accountService.verifyCredentials("alice@example.com", rawPassword);

        assertThat(response.id()).isEqualTo(account.getId());
        assertThat(response.name()).isEqualTo("Alice Johnson");
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo(Role.PASSENGER);
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void verifyCredentials_wrongPassword_throwsUnauthorized() {
        accountRepository.save(new Account(
                "Alice Johnson",
                "alice@example.com",
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("StrongPass123"),
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));

        assertThatThrownBy(() -> accountService.verifyCredentials("alice@example.com", "WrongPassword!"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException exception = (ResponseStatusException) ex;
                    assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
                    assertThat(exception.getReason()).isEqualTo("Invalid email or password.");
                });
    }

    @Test
    void verifyCredentials_unknownEmail_throwsUnauthorized() {
        assertThatThrownBy(() -> accountService.verifyCredentials("missing@example.com", "StrongPass123"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException exception = (ResponseStatusException) ex;
                    assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
                    assertThat(exception.getReason()).isEqualTo("Invalid email or password.");
                });
    }

    @Test
    void verifyCredentials_mixedCaseEmail_matchesAccount() {
        String rawPassword = "StrongPass123";
        accountRepository.save(new Account(
                "Alice Johnson",
                "ALICE@EXAMPLE.COM",
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(rawPassword),
                Role.DRIVER,
                AccountStatus.ACTIVE
        ));

        AccountResponse response = accountService.verifyCredentials("alice@example.com", rawPassword);

        assertThat(response.email()).isEqualTo("ALICE@EXAMPLE.COM");
        assertThat(response.role()).isEqualTo(Role.DRIVER);
    }

    @Test
    void verifyCredentials_suspendedAccount_throwsUnauthorized() {
        accountRepository.save(new Account(
                "Alice Johnson",
                "alice@example.com",
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("StrongPass123"),
                Role.PASSENGER,
                AccountStatus.SUSPENDED
        ));

        assertThatThrownBy(() -> accountService.verifyCredentials("alice@example.com", "StrongPass123"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException exception = (ResponseStatusException) ex;
                    assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
                    assertThat(exception.getReason()).isEqualTo("Invalid email or password.");
                });
    }
}
