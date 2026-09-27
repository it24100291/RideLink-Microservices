package com.example.accountservice.controller;
import com.example.accountservice.service.TokenAuthenticationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {
    private final TokenAuthenticationService authentication;
    private final String serviceKey;
    public InternalAuthController(TokenAuthenticationService authentication,
            @Value("${integration.account-service-key:}") String serviceKey) {
        this.authentication = authentication; this.serviceKey = serviceKey;
    }
    @PostMapping("/introspect")
    public Identity introspect(
            @RequestHeader(value = "X-Service-Key", required = false) String key,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (serviceKey.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service authentication is not configured.");
        if (key == null || !MessageDigest.isEqual(serviceKey.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid service credential.");
        var account = authentication.authenticate(authorization);
        return new Identity(account.getId(), account.getName(), account.getRole().name());
    }
    public record Identity(Long accountId, String name, String role) { }
}
