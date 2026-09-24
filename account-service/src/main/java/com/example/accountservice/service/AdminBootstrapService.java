package com.example.accountservice.service;

import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountStatus;
import com.example.accountservice.model.Role;
import com.example.accountservice.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Profile("dev")
@ConditionalOnProperty(prefix = "account.bootstrap.admin", name = "enabled", havingValue = "true")
public class AdminBootstrapService implements ApplicationRunner {

    private final AccountRepository accountRepository;
    private final String configuredEmail;
    private final String configuredPassword;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminBootstrapService(
            AccountRepository accountRepository,
            @Value("${account.bootstrap.admin.email:}") String configuredEmail,
            @Value("${account.bootstrap.admin.password:}") String configuredPassword
    ) {
        this.accountRepository = accountRepository;
        this.configuredEmail = configuredEmail;
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapIfNeeded();
    }

    public void bootstrapIfNeeded() {
        String email = firstNonBlank(
                System.getenv("ACCOUNT_BOOTSTRAP_ADMIN_EMAIL"),
                configuredEmail
        );
        String password = firstNonBlank(
                System.getenv("ACCOUNT_BOOTSTRAP_ADMIN_PASSWORD"),
                configuredPassword
        );

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Admin bootstrap email is missing. Set ACCOUNT_BOOTSTRAP_ADMIN_EMAIL.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Admin bootstrap password is missing. Set ACCOUNT_BOOTSTRAP_ADMIN_PASSWORD.");
        }

        if (accountRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        Account admin = new Account(
                "System Admin",
                email.trim(),
                passwordEncoder.encode(password),
                Role.ADMIN,
                AccountStatus.ACTIVE
        );
        accountRepository.save(admin);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
