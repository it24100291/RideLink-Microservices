package com.example.accountservice.service;

import com.example.accountservice.model.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    public static final String ISSUER = "ridelink-account-service";
    public static final String AUDIENCE = "ridelink-api";
    public static final long TOKEN_TTL_SECONDS = 60L * 60L;

    private final String configuredPrivateKey;

    public JwtService(@Value("${account.jwt.private-key:}") String configuredPrivateKey) {
        this.configuredPrivateKey = configuredPrivateKey;
    }

    public String generateToken(Long accountId, Role role, Instant issuedAt, Instant expiresAt) {
        PrivateKey privateKey = loadPrivateKey();
        return Jwts.builder()
                .setIssuer(ISSUER)
                .setAudience(AUDIENCE)
                .setSubject(accountId.toString())
                .claim("role", role.name())
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    private PrivateKey loadPrivateKey() {
        String pem = Optional.ofNullable(System.getenv("ACCOUNT_JWT_PRIVATE_KEY"))
                .filter(value -> !value.isBlank())
                .orElse(configuredPrivateKey);

        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException(
                    "JWT signing configuration is missing. Set ACCOUNT_JWT_PRIVATE_KEY or account.jwt.private-key."
            );
        }

        String cleanedPem = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        try {
            byte[] decoded = Base64.getDecoder().decode(cleanedPem);
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "JWT private key is invalid. Provide a PKCS#8 RSA private key in ACCOUNT_JWT_PRIVATE_KEY or account.jwt.private-key.",
                    ex
            );
        }
    }
}
