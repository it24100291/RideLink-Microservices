package com.example.accountservice.service;

import com.example.accountservice.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
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
    private final String configuredPublicKey;

    public JwtService(
            @Value("${account.jwt.private-key:}") String configuredPrivateKey,
            @Value("${account.jwt.public-key:}") String configuredPublicKey
    ) {
        this.configuredPrivateKey = configuredPrivateKey;
        this.configuredPublicKey = configuredPublicKey;
    }

    public String generateToken(Long accountId, Role role, Instant issuedAt, Instant expiresAt) {
        return generateToken(accountId, role, 0L, issuedAt, expiresAt);
    }

    public String generateToken(Long accountId, Role role, long tokenVersion, Instant issuedAt, Instant expiresAt) {
        PrivateKey privateKey = loadPrivateKey();
        return Jwts.builder()
                .setIssuer(ISSUER)
                .setAudience(AUDIENCE)
                .setSubject(accountId.toString())
                .claim("role", role.name())
                .claim("tokenVersion", tokenVersion)
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public Claims validateBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw unauthorized("Authentication required.");
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isBlank()) {
            throw unauthorized("Authentication required.");
        }

        try {
            Jws<Claims> parsed = Jwts.parserBuilder()
                    .setSigningKey(loadPublicKey())
                    .requireIssuer(ISSUER)
                    .requireAudience(AUDIENCE)
                    .build()
                    .parseClaimsJws(token);

            if (!"RS256".equalsIgnoreCase(parsed.getHeader().getAlgorithm())) {
                throw unauthorized("Invalid or expired token.");
            }

            Claims claims = parsed.getBody();
            if (claims.getSubject() == null || claims.getSubject().isBlank()) {
                throw unauthorized("Invalid or expired token.");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw unauthorized("Invalid or expired token.");
        }
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

        String cleanedPem = cleanPem(pem);

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

    private PublicKey loadPublicKey() {
        String pem = Optional.ofNullable(System.getenv("ACCOUNT_JWT_PUBLIC_KEY"))
                .filter(value -> !value.isBlank())
                .orElse(configuredPublicKey);

        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException(
                    "JWT verification configuration is missing. Set ACCOUNT_JWT_PUBLIC_KEY or account.jwt.public-key."
            );
        }

        String cleanedPem = cleanPem(pem);

        try {
            byte[] decoded = Base64.getDecoder().decode(cleanedPem);
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "JWT public key is invalid. Provide a valid RSA public key in ACCOUNT_JWT_PUBLIC_KEY or account.jwt.public-key.",
                    ex
            );
        }
    }

    private String cleanPem(String pem) {
        return pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
