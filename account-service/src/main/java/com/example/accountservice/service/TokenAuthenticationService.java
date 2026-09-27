package com.example.accountservice.service;
import com.example.accountservice.model.Account;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TokenAuthenticationService {
    private final JwtService jwt;
    private final AccountService accounts;
    public TokenAuthenticationService(JwtService jwt, AccountService accounts) {
        this.jwt = jwt; this.accounts = accounts;
    }
    public Account authenticate(String header) {
        var claims = jwt.validateBearerToken(header);
        long id;
        try { id = Long.parseLong(claims.getSubject()); }
        catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token subject."); }
        Account account;
        try { account = accounts.requireActiveAccount(id); }
        catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account no longer exists.");
            throw ex;
        }
        Object version = claims.get("tokenVersion");
        long value = version == null ? 0 : version instanceof Number n ? n.longValue() : -1;
        if (value != account.getTokenVersion() || !account.getRole().name().equals(claims.get("role", String.class)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has been revoked. Log in again.");
        return account;
    }
}
