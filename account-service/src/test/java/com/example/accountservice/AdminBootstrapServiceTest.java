package com.example.accountservice;

import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountStatus;
import com.example.accountservice.model.Role;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.service.AdminBootstrapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "account.bootstrap.admin.enabled=true",
        "account.bootstrap.admin.email=admin@example.com",
        "account.bootstrap.admin.password=LocalAdminPass!123"
})
class AdminBootstrapDefaultDisabledTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired(required = false)
    private AdminBootstrapService adminBootstrapService;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void bootstrap_isDisabledByDefault() {
        assertThat(adminBootstrapService).isNull();
        assertThat(accountRepository.count()).isZero();
    }
}

@SpringBootTest(properties = {
        "account.bootstrap.admin.enabled=true",
        "account.bootstrap.admin.email=admin@example.com",
        "account.bootstrap.admin.password=LocalAdminPass!123"
})
@ActiveProfiles("dev")
class AdminBootstrapEnabledTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AdminBootstrapService adminBootstrapService;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void bootstrap_createsAdminWhenExplicitlyEnabled() {
        adminBootstrapService.bootstrapIfNeeded();

        Account account = accountRepository.findByEmailIgnoreCase("admin@example.com").orElseThrow();
        assertThat(account.getRole()).isEqualTo(Role.ADMIN);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getName()).isEqualTo("System Admin");
        assertThat(BCrypt.checkpw("LocalAdminPass!123", account.getPassword())).isTrue();
    }
}

@SpringBootTest(properties = {
        "account.bootstrap.admin.enabled=true",
        "account.bootstrap.admin.email=admin@example.com",
        "account.bootstrap.admin.password=LocalAdminPass!123"
})
@ActiveProfiles("dev")
class AdminBootstrapDuplicateTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AdminBootstrapService adminBootstrapService;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void bootstrap_doesNotCreateDuplicatesOnRestart() {
        adminBootstrapService.bootstrapIfNeeded();
        adminBootstrapService.bootstrapIfNeeded();

        assertThat(accountRepository.findAll()).hasSize(1);
        assertThat(accountRepository.findByEmailIgnoreCase("admin@example.com")).isPresent();
    }
}
