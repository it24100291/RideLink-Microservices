package com.example.accountservice;

import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountStatus;
import com.example.accountservice.model.Role;
import com.example.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "account.jwt.private-key=")
@AutoConfigureMockMvc
class MissingJwtConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        accountRepository.save(new Account(
                "Alice Johnson",
                "alice@example.com",
                new BCryptPasswordEncoder().encode("StrongPass123"),
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));
    }

    @Test
    void login_withoutSigningKey_returns500() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"StrongPass123\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("JWT signing configuration is missing. Set ACCOUNT_JWT_PRIVATE_KEY or account.jwt.private-key."));
    }
}
