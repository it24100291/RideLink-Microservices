package com.example.accountservice;

import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountStatus;
import com.example.accountservice.model.Role;
import com.example.accountservice.repository.AccountRepository;
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
