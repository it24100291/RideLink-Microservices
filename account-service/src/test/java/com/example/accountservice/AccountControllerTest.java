package com.example.accountservice;

import com.example.accountservice.model.Account;
import com.example.accountservice.model.Role;
import com.example.accountservice.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void register_passenger_success() throws Exception {
        var request = new com.example.accountservice.dto.RegisterAccountRequest(
                "Alice Johnson",
                "alice@example.com",
                "Secret123",
                Role.PASSENGER
        );

        String response = mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice Johnson"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("PASSENGER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("Secret123");

        Account saved = accountRepository.findByEmailIgnoreCase("alice@example.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("Secret123");
        assertThat(BCrypt.checkpw("Secret123", saved.getPassword())).isTrue();
    }

    @Test
    void register_driver_success() throws Exception {
        var request = new com.example.accountservice.dto.RegisterAccountRequest(
                "Bob Smith",
                "bob@example.com",
                "StrongPass123",
                Role.DRIVER
        );

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("bob@example.com"))
                .andExpect(jsonPath("$.role").value("DRIVER"));

        Account saved = accountRepository.findByEmailIgnoreCase("bob@example.com").orElseThrow();
        assertThat(saved.getPassword()).isNotBlank();
        assertThat(BCrypt.checkpw("StrongPass123", saved.getPassword())).isTrue();
    }

    @Test
    void register_admin_returnsBadRequest() throws Exception {
        var request = new com.example.accountservice.dto.RegisterAccountRequest(
                "Admin User",
                "admin@example.com",
                "Secret123",
                Role.ADMIN
        );

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_invalidEmail_returnsBadRequest() throws Exception {
        var request = new com.example.accountservice.dto.RegisterAccountRequest(
                "Bad User",
                "not-an-email",
                "Secret123",
                Role.PASSENGER
        );

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingRole_returnsBadRequest() throws Exception {
        String json = "{\"name\":\"No Role\",\"email\":\"norole@example.com\",\"password\":\"Secret123\"}";

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_unknownRole_returnsBadRequest() throws Exception {
        String json = "{\"name\":\"Bad Role\",\"email\":\"badrole@example.com\",\"password\":\"Secret123\",\"role\":\"MANAGER\"}";

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateEmail_caseInsensitive_returnsConflict() throws Exception {
        var firstRequest = new com.example.accountservice.dto.RegisterAccountRequest(
                "Alice Johnson",
                "alice@example.com",
                "Secret123",
                Role.PASSENGER
        );

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        var secondRequest = new com.example.accountservice.dto.RegisterAccountRequest(
                "Alice Johnson 2",
                "ALICE@EXAMPLE.COM",
                "AnotherSecret456",
                Role.DRIVER
        );

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_hashesPasswordBeforeSaving() throws Exception {
        var request = new com.example.accountservice.dto.RegisterAccountRequest(
                "Charlie Brown",
                "charlie@example.com",
                "StrongPass123",
                Role.DRIVER
        );

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Account saved = accountRepository.findByEmailIgnoreCase("charlie@example.com").orElseThrow();
        assertThat(saved.getPassword()).isNotBlank();
        assertThat(saved.getPassword()).isNotEqualTo("StrongPass123");
        assertThat(BCrypt.checkpw("StrongPass123", saved.getPassword())).isTrue();
    }
}
