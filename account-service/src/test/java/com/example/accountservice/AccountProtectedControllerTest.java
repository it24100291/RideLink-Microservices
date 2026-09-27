package com.example.accountservice;

import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountStatus;
import com.example.accountservice.model.Role;
import com.example.accountservice.repository.AccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountProtectedControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static PrivateKey privateKey;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @DynamicPropertySource
    static void registerJwtKeys(DynamicPropertyRegistry registry) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        privateKey = keyPair.getPrivate();

        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
        String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";

        registry.add("account.jwt.private-key", () -> privateKeyPem);
        registry.add("account.jwt.public-key", () -> publicKeyPem);
    }

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void openApiSpec_usesBearerSecuritySchemeWithoutHeaderParameter() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = OBJECT_MAPPER.readTree(json);
        JsonNode securityScheme = root.path("components").path("securitySchemes").path("bearerAuth");
        org.assertj.core.api.Assertions.assertThat(securityScheme.path("type").asText()).isEqualTo("http");
        org.assertj.core.api.Assertions.assertThat(securityScheme.path("scheme").asText()).isEqualTo("bearer");

        JsonNode parameters = root.path("paths").path("/api/accounts/me").path("get").path("parameters");
        boolean hasAuthorizationHeaderParam = false;
        if (parameters.isArray()) {
            for (JsonNode parameter : parameters) {
                if ("Authorization".equals(parameter.path("name").asText()) && "header".equals(parameter.path("in").asText())) {
                    hasAuthorizationHeaderParam = true;
                    break;
                }
            }
        }
        org.assertj.core.api.Assertions.assertThat(hasAuthorizationHeaderParam).isFalse();
    }

    @Test
    void me_get_returnsAuthenticatedProfile() throws Exception {
        Account account = accountRepository.save(new Account(
                "Alice Johnson",
                "alice@example.com",
                new BCryptPasswordEncoder().encode("StrongPass123"),
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));

        mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + signedToken(account.getId(), Role.PASSENGER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.getId().intValue()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("PASSENGER"));
    }

    @Test
    void me_patch_updatesOnlyOwnName() throws Exception {
        Account primary = accountRepository.save(new Account(
                "Alice Johnson",
                "alice@example.com",
                new BCryptPasswordEncoder().encode("StrongPass123"),
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));
        Account other = accountRepository.save(new Account(
                "Other User",
                "other@example.com",
                new BCryptPasswordEncoder().encode("StrongPass123"),
                Role.DRIVER,
                AccountStatus.ACTIVE
        ));

        mockMvc.perform(patch("/api/accounts/me")
                        .header("Authorization", "Bearer " + signedToken(primary.getId(), Role.PASSENGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + other.getId() + "\",\"name\":\"Updated Me\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Me"))
                .andExpect(jsonPath("$.id").value(primary.getId().intValue()));

        Account freshPrimary = accountRepository.findById(primary.getId()).orElseThrow();
        Account freshOther = accountRepository.findById(other.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(freshPrimary.getName()).isEqualTo("Updated Me");
        org.assertj.core.api.Assertions.assertThat(freshOther.getName()).isEqualTo("Other User");
    }

    @Test
    void suspendedAccount_cannotAccessProtectedEndpoints() throws Exception {
        Account account = accountRepository.save(new Account(
                "Alice Johnson",
                "alice@example.com",
                new BCryptPasswordEncoder().encode("StrongPass123"),
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));

        String token = signedToken(account.getId(), Role.PASSENGER);
        account.setStatus(AccountStatus.SUSPENDED);
        accountRepository.save(account);

        mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/accounts/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Name\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingToken_returns401() throws Exception {
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSignature_returns401() throws Exception {
        Account account = accountRepository.save(new Account(
                "Alice Johnson",
                "alice@example.com",
                new BCryptPasswordEncoder().encode("StrongPass123"),
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));

        String invalidToken = signedToken(account.getId(), Role.PASSENGER, "wrong-key");

        mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_returns401() throws Exception {
        Account account = accountRepository.save(new Account(
                "Alice Johnson",
                "alice@example.com",
                new BCryptPasswordEncoder().encode("StrongPass123"),
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));

        String expired = Jwts.builder()
                .setIssuer("ridelink-account-service")
                .setAudience("ridelink-api")
                .setSubject(account.getId().toString())
                .claim("role", Role.PASSENGER.name())
                .setIssuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .setExpiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidIssuer_returns401() throws Exception {
        Account account = accountRepository.save(new Account(
                "Alice Johnson",
                "alice@example.com",
                new BCryptPasswordEncoder().encode("StrongPass123"),
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));

        String badIssuer = Jwts.builder()
                .setIssuer("bad-issuer")
                .setAudience("ridelink-api")
                .setSubject(account.getId().toString())
                .claim("role", Role.PASSENGER.name())
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + badIssuer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidAudience_returns401() throws Exception {
        Account account = accountRepository.save(new Account(
                "Alice Johnson",
                "alice@example.com",
                new BCryptPasswordEncoder().encode("StrongPass123"),
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));

        String badAudience = Jwts.builder()
                .setIssuer("ridelink-account-service")
                .setAudience("bad-audience")
                .setSubject(account.getId().toString())
                .claim("role", Role.PASSENGER.name())
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + badAudience))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminStatusPatch_requiresAdminRole() throws Exception {
        Account target = accountRepository.save(new Account(
                "Target User",
                "target@example.com",
                new BCryptPasswordEncoder().encode("StrongPass123"),
                Role.DRIVER,
                AccountStatus.ACTIVE
        ));

        Account passenger = accountRepository.save(new Account(
                "Passenger User",
                "passenger@example.com",
                new BCryptPasswordEncoder().encode("StrongPass123"),
                Role.PASSENGER,
                AccountStatus.ACTIVE
        ));

        mockMvc.perform(patch("/api/accounts/{id}/status", target.getId())
                        .header("Authorization", "Bearer " + signedToken(passenger.getId(), Role.PASSENGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminStatusPatch_allowsAdminToChangeStatus() throws Exception {
        Account target = accountRepository.save(new Account(
                "Target User",
                "target@example.com",
                new BCryptPasswordEncoder().encode("StrongPass123"),
                Role.DRIVER,
                AccountStatus.ACTIVE
        ));

        Account admin = accountRepository.save(new Account(
                "Admin User",
                "admin@example.com",
                new BCryptPasswordEncoder().encode("StrongPass123"),
                Role.ADMIN,
                AccountStatus.ACTIVE
        ));

        mockMvc.perform(patch("/api/accounts/{id}/status", target.getId())
                        .header("Authorization", "Bearer " + signedToken(admin.getId(), Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        Account refreshed = accountRepository.findById(target.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(refreshed.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    void profileChangesEmailAndNameAndLoginUsesNewEmail() throws Exception {
        Account user = createAccount("alice@example.com", Role.PASSENGER);
        mockMvc.perform(patch("/api/accounts/me")
                .header("Authorization", "Bearer " + signedToken(user.getId(), user.getRole()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" Updated Alice \",\"email\":\"NEW@example.com\",\"currentPassword\":\"StrongPass123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Alice"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.role").value("PASSENGER"))
                .andExpect(jsonPath("$.password").doesNotExist());
        login("alice@example.com", "StrongPass123", 401);
        login("new@example.com", "StrongPass123", 200);
    }

    @Test
    void profileRejectsInvalidDuplicateAndUnauthorizedEmailChangesWithoutPartialUpdates() throws Exception {
        Account user = createAccount("alice@example.com", Role.PASSENGER);
        createAccount("taken@example.com", Role.DRIVER);
        String[] bodies = {"{}", "{\"name\":\" \"}", "{\"email\":\"bad-email\"}",
                "{\"email\":\"new@example.com\"}",
                "{\"email\":\"new@example.com\",\"currentPassword\":\"wrong\"}",
                "{\"name\":\"Changed\",\"email\":\"TAKEN@example.com\",\"currentPassword\":\"StrongPass123\"}"};
        for (int i = 0; i < bodies.length; i++) {
            mockMvc.perform(patch("/api/accounts/me")
                    .header("Authorization", "Bearer " + signedToken(user.getId(), user.getRole()))
                    .contentType(MediaType.APPLICATION_JSON).content(bodies[i]))
                    .andExpect(status().is(i == bodies.length - 1 ? 409 : 400));
        }
        Account unchanged = accountRepository.findById(user.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(unchanged.getEmail()).isEqualTo("alice@example.com");
        org.assertj.core.api.Assertions.assertThat(unchanged.getName()).isEqualTo("Test User");
    }

    @Test
    void passwordChangeHashesPasswordRevokesOldTokenAndAllowsNewLogin() throws Exception {
        Account user = createAccount("alice@example.com", Role.PASSENGER);
        String oldToken = signedToken(user.getId(), user.getRole());
        mockMvc.perform(patch("/api/accounts/me/password")
                .header("Authorization", "Bearer " + oldToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"StrongPass123\",\"newPassword\":\"NewStrongPass456\"}"))
                .andExpect(status().isNoContent());
        Account changed = accountRepository.findById(user.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(new BCryptPasswordEncoder().matches("NewStrongPass456", changed.getPassword())).isTrue();
        mockMvc.perform(get("/api/accounts/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        login("alice@example.com", "StrongPass123", 401);
        String newToken = login("alice@example.com", "NewStrongPass456", 200);
        mockMvc.perform(get("/api/accounts/me").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
    }

    @Test
    void passwordChangeRejectsWrongCurrentWeakSameAndOverlongPasswords() throws Exception {
        Account user = createAccount("alice@example.com", Role.PASSENGER);
        String[][] passwords = {{"wrong", "ValidPass123"}, {"StrongPass123", "short"},
                {"StrongPass123", "StrongPass123"}, {"StrongPass123", "é".repeat(40)}};
        for (String[] pair : passwords) {
            mockMvc.perform(patch("/api/accounts/me/password")
                    .header("Authorization", "Bearer " + signedToken(user.getId(), user.getRole()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(java.util.Map.of("currentPassword", pair[0], "newPassword", pair[1]))))
                    .andExpect(status().isBadRequest());
        }
        login("alice@example.com", "StrongPass123", 200);
    }

    @Test
    void adminCanChangeRolesAndOldTokensAreRevoked() throws Exception {
        Account admin = createAccount("admin@example.com", Role.ADMIN);
        Account target = createAccount("target@example.com", Role.PASSENGER);
        String oldToken = signedToken(target.getId(), target.getRole());
        for (Role role : new Role[]{Role.DRIVER, Role.ADMIN, Role.PASSENGER}) {
            mockMvc.perform(patch("/api/accounts/{id}/role", target.getId())
                    .header("Authorization", "Bearer " + signedToken(admin.getId(), admin.getRole()))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"" + role + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.role").value(role.name()));
        }
        mockMvc.perform(get("/api/accounts/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        String newToken = login("target@example.com", "StrongPass123", 200);
        mockMvc.perform(get("/api/accounts/me").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("PASSENGER"));
    }

    @Test
    void roleManagementRejectsNonAdminsSelfDemotionMissingTargetsAndInvalidRoles() throws Exception {
        Account admin = createAccount("admin@example.com", Role.ADMIN);
        Account passenger = createAccount("passenger@example.com", Role.PASSENGER);
        Account driver = createAccount("driver@example.com", Role.DRIVER);
        for (Account caller : new Account[]{passenger, driver}) {
            mockMvc.perform(patch("/api/accounts/{id}/role", caller.getId())
                    .header("Authorization", "Bearer " + signedToken(caller.getId(), Role.ADMIN))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                    .andExpect(status().isForbidden());
        }
        String token = signedToken(admin.getId(), Role.ADMIN);
        mockMvc.perform(patch("/api/accounts/{id}/role", admin.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"PASSENGER\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/accounts/999999/role")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"DRIVER\"}"))
                .andExpect(status().isNotFound());
        for (String body : new String[]{"{}", "{\"role\":\"INVALID\"}"}) {
            mockMvc.perform(patch("/api/accounts/{id}/role", passenger.getId())
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void newEndpointsRequireAuthenticationAndRejectSuspendedAccounts() throws Exception {
        Account admin = createAccount("admin@example.com", Role.ADMIN);
        String token = signedToken(admin.getId(), Role.ADMIN);
        admin.setStatus(AccountStatus.SUSPENDED);
        accountRepository.saveAndFlush(admin);
        String[][] requests = {{"/api/accounts/me/password", "{\"currentPassword\":\"StrongPass123\",\"newPassword\":\"NewPassword123\"}"},
                {"/api/accounts/123/role", "{\"role\":\"DRIVER\"}"}};
        for (String[] request : requests) {
            mockMvc.perform(patch(request[0]).contentType(MediaType.APPLICATION_JSON).content(request[1]))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(patch(request[0]).header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(request[1]))
                    .andExpect(status().isUnauthorized());
        }
    }

    private Account createAccount(String email, Role role) {
        return accountRepository.saveAndFlush(new Account("Test User", email,
                new BCryptPasswordEncoder().encode("StrongPass123"), role, AccountStatus.ACTIVE));
    }

    private String login(String email, String password, int expectedStatus) throws Exception {
        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(java.util.Map.of("email", email, "password", password))))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        return expectedStatus == 200 ? OBJECT_MAPPER.readTree(response).get("accessToken").asText() : null;
    }

    private static String signedToken(Long accountId, Role role) {
        return signedToken(accountId, role, null);
    }

    private static String signedToken(Long accountId, Role role, String issuerOverride) {
        Instant now = Instant.now();
        if (issuerOverride == null) {
            issuerOverride = "ridelink-account-service";
        }

        if (issuerOverride.equals("wrong-key")) {
            return Jwts.builder()
                    .setIssuer("ridelink-account-service")
                    .setAudience("ridelink-api")
                    .setSubject(accountId.toString())
                    .claim("role", role.name())
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(now.plusSeconds(3600)))
                    .signWith(generateWrongKey(), SignatureAlgorithm.RS256)
                    .compact();
        }

        return Jwts.builder()
                .setIssuer(issuerOverride)
                .setAudience("ridelink-api")
                .setSubject(accountId.toString())
                .claim("role", role.name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    private static PrivateKey generateWrongKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair().getPrivate();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
